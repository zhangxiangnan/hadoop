/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hdfs.inotify;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.fs.XAttr;
import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.FsPermission;

import java.util.List;

/**
 * Events sent by the inotify system. Note that no events are necessarily sent
 * when a file is opened for read (although a MetadataUpdateEvent will be sent
 * if the atime is updated).
 */
@InterfaceAudience.Public
@InterfaceStability.Unstable
public abstract class Event {
  public static enum EventType {
    CREATE, CLOSE, APPEND, RENAME, METADATA, UNLINK, TRUNCATE
  }

  private EventType eventType;

  public EventType getEventType() {
    return eventType;
  }

  public Event(EventType eventType) {
    this.eventType = eventType;
  }

  /**
   * Sent when a file is closed after append or create.
   */
  public static class CloseEvent extends Event {
    private String path;
    private long fileSize;
    private long timestamp;

    public CloseEvent(String path, long fileSize, long timestamp) {
      super(EventType.CLOSE);
      this.path = path;
      this.fileSize = fileSize;
      this.timestamp = timestamp;
    }

    public String getPath() {
      return path;
    }

    /**
     * The size of the closed file in bytes. May be -1 if the size is not
     * available (e.g. in the case of a close generated by a concat operation).
     */
    public long getFileSize() {
      return fileSize;
    }

    /**
     * The time when this event occurred, in milliseconds since the epoch.
     */
    public long getTimestamp() {
      return timestamp;
    }
  }

  /**
   * Sent when a new file is created (including overwrite).
   */
  public static class CreateEvent extends Event {

    public static enum INodeType {
      FILE, DIRECTORY, SYMLINK;
    }

    private INodeType iNodeType;
    private String path;
    private long ctime;
    private int replication;
    private String ownerName;
    private String groupName;
    private FsPermission perms;
    private String symlinkTarget;
    private boolean overwrite;
    private long defaultBlockSize;

    public static class Builder {
      private INodeType iNodeType;
      private String path;
      private long ctime;
      private int replication;
      private String ownerName;
      private String groupName;
      private FsPermission perms;
      private String symlinkTarget;
      private boolean overwrite;
      private long defaultBlockSize = 0;

      public Builder iNodeType(INodeType type) {
        this.iNodeType = type;
        return this;
      }

      public Builder path(String path) {
        this.path = path;
        return this;
      }

      public Builder ctime(long ctime) {
        this.ctime = ctime;
        return this;
      }

      public Builder replication(int replication) {
        this.replication = replication;
        return this;
      }

      public Builder ownerName(String ownerName) {
        this.ownerName = ownerName;
        return this;
      }

      public Builder groupName(String groupName) {
        this.groupName = groupName;
        return this;
      }

      public Builder perms(FsPermission perms) {
        this.perms = perms;
        return this;
      }

      public Builder symlinkTarget(String symlinkTarget) {
        this.symlinkTarget = symlinkTarget;
        return this;
      }

      public Builder overwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
      }

      public Builder defaultBlockSize(long defaultBlockSize) {
        this.defaultBlockSize = defaultBlockSize;
        return this;
      }

      public CreateEvent build() {
        return new CreateEvent(this);
      }
    }

    private CreateEvent(Builder b) {
      super(EventType.CREATE);
      this.iNodeType = b.iNodeType;
      this.path = b.path;
      this.ctime = b.ctime;
      this.replication = b.replication;
      this.ownerName = b.ownerName;
      this.groupName = b.groupName;
      this.perms = b.perms;
      this.symlinkTarget = b.symlinkTarget;
      this.overwrite = b.overwrite;
      this.defaultBlockSize = b.defaultBlockSize;
    }

    public INodeType getiNodeType() {
      return iNodeType;
    }

    public String getPath() {
      return path;
    }

    /**
     * Creation time of the file, directory, or symlink.
     */
    public long getCtime() {
      return ctime;
    }

    /**
     * Replication is zero if the CreateEvent iNodeType is directory or symlink.
     */
    public int getReplication() {
      return replication;
    }

    public String getOwnerName() {
      return ownerName;
    }

    public String getGroupName() {
      return groupName;
    }

    public FsPermission getPerms() {
      return perms;
    }

    /**
     * Symlink target is null if the CreateEvent iNodeType is not symlink.
     */
    public String getSymlinkTarget() {
      return symlinkTarget;
    }

    public boolean getOverwrite() {
      return overwrite;
    }

    public long getDefaultBlockSize() {
      return defaultBlockSize;
    }
  }

  /**
   * Sent when there is an update to directory or file (none of the metadata
   * tracked here applies to symlinks) that is not associated with another
   * inotify event. The tracked metadata includes atime/mtime, replication,
   * owner/group, permissions, ACLs, and XAttributes. Fields not relevant to the
   * metadataType of the MetadataUpdateEvent will be null or will have their default
   * values.
   */
  public static class MetadataUpdateEvent extends Event {

    public static enum MetadataType {
      TIMES, REPLICATION, OWNER, PERMS, ACLS, XATTRS;
    }

    private String path;
    private MetadataType metadataType;
    private long mtime;
    private long atime;
    private int replication;
    private String ownerName;
    private String groupName;
    private FsPermission perms;
    private List<AclEntry> acls;
    private List<XAttr> xAttrs;
    private boolean xAttrsRemoved;

    public static class Builder {
      private String path;
      private MetadataType metadataType;
      private long mtime;
      private long atime;
      private int replication;
      private String ownerName;
      private String groupName;
      private FsPermission perms;
      private List<AclEntry> acls;
      private List<XAttr> xAttrs;
      private boolean xAttrsRemoved;

      public Builder path(String path) {
        this.path = path;
        return this;
      }

      public Builder metadataType(MetadataType type) {
        this.metadataType = type;
        return this;
      }

      public Builder mtime(long mtime) {
        this.mtime = mtime;
        return this;
      }

      public Builder atime(long atime) {
        this.atime = atime;
        return this;
      }

      public Builder replication(int replication) {
        this.replication = replication;
        return this;
      }

      public Builder ownerName(String ownerName) {
        this.ownerName = ownerName;
        return this;
      }

      public Builder groupName(String groupName) {
        this.groupName = groupName;
        return this;
      }

      public Builder perms(FsPermission perms) {
        this.perms = perms;
        return this;
      }

      public Builder acls(List<AclEntry> acls) {
        this.acls = acls;
        return this;
      }

      public Builder xAttrs(List<XAttr> xAttrs) {
        this.xAttrs = xAttrs;
        return this;
      }

      public Builder xAttrsRemoved(boolean xAttrsRemoved) {
        this.xAttrsRemoved = xAttrsRemoved;
        return this;
      }

      public MetadataUpdateEvent build() {
        return new MetadataUpdateEvent(this);
      }
    }

    private MetadataUpdateEvent(Builder b) {
      super(EventType.METADATA);
      this.path = b.path;
      this.metadataType = b.metadataType;
      this.mtime = b.mtime;
      this.atime = b.atime;
      this.replication = b.replication;
      this.ownerName = b.ownerName;
      this.groupName = b.groupName;
      this.perms = b.perms;
      this.acls = b.acls;
      this.xAttrs = b.xAttrs;
      this.xAttrsRemoved = b.xAttrsRemoved;
    }

    public String getPath() {
      return path;
    }

    public MetadataType getMetadataType() {
      return metadataType;
    }

    public long getMtime() {
      return mtime;
    }

    public long getAtime() {
      return atime;
    }

    public int getReplication() {
      return replication;
    }

    public String getOwnerName() {
      return ownerName;
    }

    public String getGroupName() {
      return groupName;
    }

    public FsPermission getPerms() {
      return perms;
    }

    /**
     * The full set of ACLs currently associated with this file or directory.
     * May be null if all ACLs were removed.
     */
    public List<AclEntry> getAcls() {
      return acls;
    }

    public List<XAttr> getxAttrs() {
      return xAttrs;
    }

    /**
     * Whether the xAttrs returned by getxAttrs() were removed (as opposed to
     * added).
     */
    public boolean isxAttrsRemoved() {
      return xAttrsRemoved;
    }

  }

  /**
   * Sent when a file, directory, or symlink is renamed.
   */
  public static class RenameEvent extends Event {
    private String srcPath;
    private String dstPath;
    private long timestamp;

    public static class Builder {
      private String srcPath;
      private String dstPath;
      private long timestamp;

      public Builder srcPath(String srcPath) {
        this.srcPath = srcPath;
        return this;
      }

      public Builder dstPath(String dstPath) {
        this.dstPath = dstPath;
        return this;
      }

      public Builder timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
      }

      public RenameEvent build() {
        return new RenameEvent(this);
      }
    }

    private RenameEvent(Builder builder) {
      super(EventType.RENAME);
      this.srcPath = builder.srcPath;
      this.dstPath = builder.dstPath;
      this.timestamp = builder.timestamp;
    }

    public String getSrcPath() {
      return srcPath;
    }

    public String getDstPath() {
      return dstPath;
    }

    /**
     * The time when this event occurred, in milliseconds since the epoch.
     */
    public long getTimestamp() {
      return timestamp;
    }
  }

  /**
   * Sent when an existing file is opened for append.
   */
  public static class AppendEvent extends Event {
    private String path;
    private boolean newBlock;

    public static class Builder {
      private String path;
      private boolean newBlock;

      public Builder path(String path) {
        this.path = path;
        return this;
      }

      public Builder newBlock(boolean newBlock) {
        this.newBlock = newBlock;
        return this;
      }

      public AppendEvent build() {
        return new AppendEvent(this);
      }
    }

    private AppendEvent(Builder b) {
      super(EventType.APPEND);
      this.path = b.path;
      this.newBlock = b.newBlock;
    }

    public String getPath() {
      return path;
    }

    public boolean toNewBlock() {
      return newBlock;
    }
  }

  /**
   * Sent when a file, directory, or symlink is deleted.
   */
  public static class UnlinkEvent extends Event {
    private String path;
    private long timestamp;

    public static class Builder {
      private String path;
      private long timestamp;

      public Builder path(String path) {
        this.path = path;
        return this;
      }

      public Builder timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
      }

      public UnlinkEvent build() {
        return new UnlinkEvent(this);
      }
    }

    private UnlinkEvent(Builder builder) {
      super(EventType.UNLINK);
      this.path = builder.path;
      this.timestamp = builder.timestamp;
    }

    public String getPath() {
      return path;
    }

    /**
     * The time when this event occurred, in milliseconds since the epoch.
     */
    public long getTimestamp() {
      return timestamp;
    }
  }

  /**
   * Sent when a file is truncated.
   */
  public static class TruncateEvent extends Event {
    private String path;
    private long fileSize;
    private long timestamp;


    public TruncateEvent(String path, long fileSize, long timestamp) {
      super(EventType.TRUNCATE);
      this.path = path;
      this.fileSize = fileSize;
      this.timestamp = timestamp;
    }

    public String getPath() {
      return path;
    }

    /**
     * The size of the truncated file in bytes.
     */
    public long getFileSize() {
      return fileSize;
    }

    /**
     * The time when this event occurred, in milliseconds since the epoch.
     */
    public long getTimestamp() {
      return timestamp;
    }
  }
}
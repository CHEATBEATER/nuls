/**
 * MIT License
 * *
 * Copyright (c) 2017-2018 nuls.io
 * *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.network.entity;

import io.nuls.core.utils.date.TimeService;
import io.nuls.core.utils.str.StringUtils;
import io.nuls.network.message.entity.VersionEvent;
import io.nuls.protocol.model.BaseNulsData;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author vivi
 * @Date 2017.11.01
 */
public class Node extends BaseNulsData {

    private transient String id;

    private String ip;

    private transient Integer port;

    private Integer severPort = 0;

    private int magicNumber;

    private transient String channelId;

    private transient Long lastTime;

    private transient Long lastFailTime;

    private transient Integer failCount;

    private transient Set<String> groupSet;

    /**
     * 1: inNode ,  2: outNode
     */
    public final static transient int IN = 1;
    public final static transient int OUT = 2;
    private transient int type;

    /**
     * 0: wait , 1: connecting, 2: handshake 3: close
     */
    public final static transient int WAIT = 0;
    public final static transient int CONNECT = 1;
    public final static transient int HANDSHAKE = 2;
    public final static transient int CLOSE = 3;
    public final static transient int BAD = 4;
    private volatile transient int status;

    private transient boolean canConnect;

    private volatile transient VersionEvent versionMessage;

    public Node() {
        this.status = CLOSE;
        groupSet = ConcurrentHashMap.newKeySet();
    }

    public Node(String ip, int port, int type) {
        this();
        this.ip = ip;
        this.port = port;
        if (type == Node.OUT) {
            this.severPort = port;
        }
        this.type = type;
    }

    public Node(String ip, int port, int severPort, int type) {
        this(ip, port, type);
        this.severPort = severPort;
    }

    public Node(String id, String ip, int port, int serverPort, int type) {
        this(ip, port, serverPort, type);
        this.id = id;
    }

    public void destroy() {
        this.lastFailTime = TimeService.currentTimeMillis();
        this.setFailCount(this.getFailCount() + 1);
        this.status = Node.CLOSE;
    }

    public boolean isHandShake() {
        return this.status == Node.HANDSHAKE;
    }

    public boolean isAlive() {
        return this.status == Node.CONNECT || status == Node.HANDSHAKE;
    }

    public void addToGroup(NodeGroup nodeGroup) {
        if (nodeGroup != null) {
            this.groupSet.add(nodeGroup.getName());
        }
    }

    public void removeFromGroup(NodeGroup nodeGroup) {
        if (nodeGroup != null) {
            this.groupSet.remove(nodeGroup.getName());
        }
    }

    public void addGroup(String groupName) {
        this.groupSet.add(groupName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("id:" + getId() + ",");
        sb.append("ip: '" + getIp() + "',");
        sb.append("port: " + getPort() + ",");
        sb.append("type:" + type + ",");
        sb.append("status:" + status + "}");

//        sb.append("{");
//        sb.append("ip: '" + getIp() + "',");
//        sb.append("port: " + getPort() + ",");
//        sb.append("id:" + getId() + ",");
//        sb.append("type:" + type + ",");
//        sb.append("status:" + status + "}");
//        if (lastTime == null) {
//            lastTime = System.currentTimeMillis();
//        }
//        sb.append("lastTime: " + DateUtil.convertDate(new Date(lastTime)) + ",");
//        sb.append("magicNumber: " + magicNumber + "}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        Node other = (Node) obj;
        if (StringUtils.isBlank(other.getId())) {
            return false;
        }
        return other.getId().equals(this.getId());
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public VersionEvent getVersionMessage() {
        return versionMessage;
    }

    public void setVersionMessage(VersionEvent versionMessage) {
        this.versionMessage = versionMessage;
    }

    public Long getLastTime() {
        return lastTime;
    }

    public void setLastTime(Long lastTime) {
        this.lastTime = lastTime;
    }

    public Integer getFailCount() {
        if (failCount == null) {
            failCount = 0;
        }
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public int getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(int magicNumber) {
        this.magicNumber = magicNumber;
    }

    public int getGroupCount(String groupName) {
        return this.groupSet.size();
    }

    public Set<String> getGroupSet() {
        return this.groupSet;
    }

    public Long getLastFailTime() {
        if (lastFailTime == null) {
            lastFailTime = 0L;
        }
        return lastFailTime;
    }

    public void setLastFailTime(Long lastFailTime) {
        this.lastFailTime = lastFailTime;
    }

    public String getId() {
        id = ip + ":" + port;
        return id;
    }

    public String getPoId() {
        if (severPort == null || severPort == 0) {
            severPort = port;
        }
        id = ip + ":" + severPort;
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getSeverPort() {
        return severPort;
    }

    public void setSeverPort(Integer severPort) {
        this.severPort = severPort;
    }

    public boolean isCanConnect() {
        return canConnect;
    }

    public void setCanConnect(boolean canConnect) {
        this.canConnect = canConnect;
    }
}

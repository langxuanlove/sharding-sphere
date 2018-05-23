package com.saaavsaaa.client.zookeeper.base;

import com.saaavsaaa.client.action.IProvider;
import com.saaavsaaa.client.election.LeaderElection;
import com.saaavsaaa.client.utility.PathUtil;
import com.saaavsaaa.client.utility.constant.Constants;
import com.saaavsaaa.client.zookeeper.transaction.ZKTransaction;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Stack;

/**
 * Created by aaa
 */
public class BaseProvider implements IProvider {
    private static final Logger logger = LoggerFactory.getLogger(BaseProvider.class);
    protected final ZooKeeper zooKeeper;
    protected final boolean watched;
    protected final List<ACL> authorities;
    protected final String rootNode;
    
    public BaseProvider(final String rootNode, final ZooKeeper zooKeeper, final boolean watched, final List<ACL> authorities){
        this.rootNode = rootNode;
        this.zooKeeper = zooKeeper;
        this.watched = watched;
        this.authorities = authorities;
    }
    
    public String getDataString(final String key) throws KeeperException, InterruptedException {
        return new String(getData(key));
    }
    
    @Override
    public byte[] getData(final String key) throws KeeperException, InterruptedException {
        return zooKeeper.getData(key, watched, null);
    }
    
    @Override
    public void getData(final String key, final AsyncCallback.DataCallback callback, final Object ctx) throws KeeperException, InterruptedException {
        zooKeeper.getData(key, watched, callback, ctx);
    }
    
    @Override
    public boolean exists(final String key) throws KeeperException, InterruptedException {
        return null != zooKeeper.exists(key, watched);
    }
    
    @Override
    public boolean exists(final String key, final Watcher watcher) throws KeeperException, InterruptedException {
        return null != zooKeeper.exists(key, watcher);
    }
    
    @Override
    public List<String> getChildren(final String key) throws KeeperException, InterruptedException {
        return zooKeeper.getChildren(key, watched);
    }
    
    @Override
    public void create(final String key, final String value, final CreateMode createMode) throws KeeperException, InterruptedException {
        try {
            zooKeeper.create(key, value.getBytes(Constants.UTF_8), authorities, createMode);
            logger.debug("BaseProvider createCurrentOnly:{}", key);
        } catch (KeeperException.NoNodeException e) {
            logger.error("BaseProvider createCurrentOnly:{}", e.getMessage(), e);
            // I don't know whether it will happen or not, if root watcher don't update rootExist timely
            if (!exists(rootNode)){
                logger.info("BaseProvider createCurrentOnly root not exist");
                Thread.sleep(50);
                this.create(key, value, createMode);
            }
        }
    }

    @Override
    public void update(final String key, final String value) throws KeeperException, InterruptedException {
        zooKeeper.setData(key, value.getBytes(Constants.UTF_8), Constants.VERSION);
    }
    
    @Override
    public void delete(final String key) throws KeeperException, InterruptedException {
        zooKeeper.delete(key, Constants.VERSION);
        logger.debug("BaseProvider deleteOnlyCurrent:{}", key);
    }
    
    @Override
    public void delete(final String key, final AsyncCallback.VoidCallback callback, final Object ctx) throws KeeperException, InterruptedException {
        zooKeeper.delete(key, Constants.VERSION, callback, ctx);
        logger.debug("BaseProvider deleteOnlyCurrent:{},ctx:{}", key, ctx);
    }
    
    
    @Override
    public String getRealPath(String path) {
        return PathUtil.getRealPath(rootNode, path);
    }
    
    @Override
    public List<String> getNecessaryPaths(final String key){
        List<String> nodes = PathUtil.getPathOrderNodes(rootNode, key);
        nodes.remove(rootNode);
        return nodes;
    }
    
    @Override
    public Stack<String> getDeletingPaths(String key) {
        return PathUtil.getPathReverseNodes(rootNode, key);
    }
    
    @Override
    public void executeContention(final LeaderElection election) throws KeeperException, InterruptedException {
        this.executeContention(rootNode, election);
    }
    
    public void executeContention(final String nodeBeCompete, final LeaderElection election) throws KeeperException, InterruptedException {
        election.executeContention(rootNode, this);
    }
    
    @Override
    public void createInTransaction(final String key, final String value, final CreateMode createMode, final ZKTransaction transaction) throws KeeperException, InterruptedException {
        transaction.create(key, value.getBytes(Constants.UTF_8), authorities, createMode);
    }
    
    public String getRootNode(){
        return rootNode;
    }
    
    public ZooKeeper getZooKeeper(){
        return zooKeeper;
    }
}

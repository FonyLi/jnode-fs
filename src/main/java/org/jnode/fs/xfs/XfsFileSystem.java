package org.jnode.fs.xfs;

import org.jnode.driver.Device;
import org.jnode.driver.block.BlockDeviceAPI;
import org.jnode.fs.*;
import org.jnode.fs.spi.AbstractFileSystem;
import org.jnode.fs.xfs.inode.INode;
import org.jnode.fs.xfs.inode.INodeFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * An XFS file system.
 *
 * @author Luke Quinane
 */
public class XfsFileSystem extends AbstractFileSystem<XfsEntry> {

    /**
     * The superblock.
     */
    private Superblock superblock;

    /**
     * The allocation group for inodes.
     */
    private AllocationGroupINode agINode;

    /**
     * The allocation group size.
     */
    private long allocationGroupSize;

    /**
     * Construct an XFS file system.
     *
     * @param device device contains file system.
     * @param type   the file system type.
     * @throws FileSystemException device is null or device has no {@link BlockDeviceAPI} defined.
     */
    public XfsFileSystem(Device device, FileSystemType<? extends FileSystem<XfsEntry>> type)
            throws FileSystemException {

        super(device, true, type);
    }

    /**
     * Reads in the file system from the block device.
     *
     * @throws FileSystemException if an error occurs reading the file system.
     */
    public final void read() throws FileSystemException {
        superblock = new Superblock(this);
        agINode = new AllocationGroupINode(this);
        allocationGroupSize = superblock.getBlockSize() * superblock.getTotalBlocks() / superblock.getAGCount();
    }

    /**
     * Reads in the file system from the block device.
     * TODO remove, or maybe refactor somehow
     *
     * @param absoluteINodeNumber the absolute inode number.
     * @return the {@link INode}.
     * @throws IOException if an error occurs reading the file system.
     */
    public INode getINode(long absoluteINodeNumber) throws IOException {
        long offset = getINodeAbsoluteOffset(absoluteINodeNumber);

        // Reserve the space to read the iNode
        ByteBuffer allocate = ByteBuffer.allocate(getSuperblock().getInodeSize());

        // Read the iNode data
        getApi().read(offset, allocate);
        return INodeFactory.create(absoluteINodeNumber, allocate.array(), 0, this);
    }

    public long getINodeAbsoluteOffset(long absoluteINodeNumber) {
        long numberOfRelativeINodeBits = getSuperblock().getAGSizeLog2() + getSuperblock().getINodePerBlockLog2();
        int allocationGroupIndex = (int) (absoluteINodeNumber >> numberOfRelativeINodeBits);
        long allocationGroupBlockNumber = (long) allocationGroupIndex * getSuperblock().getAGSize();
        long relativeINodeNumber = absoluteINodeNumber & (((long) 1 << numberOfRelativeINodeBits) - 1);

        // Calculate the offset of the iNode number.
        return (allocationGroupBlockNumber * getSuperblock().getBlockSize()) + (relativeINodeNumber * getSuperblock().getInodeSize());
    }

    /**
     * Gets the total space value stored in the superblock.
     */
    @Override
    public long getTotalSpace() {
        return superblock.getBlockSize() * superblock.getTotalBlocks();
    }

    /**
     * Gets the total free space value stored in the superblock.
     */
    @Override
    public long getFreeSpace() {
        return superblock.getBlockSize() * superblock.getFreeBlocks();
    }

    /**
     * Gets the total usable space value.
     */
    @Override
    public long getUsableSpace() {
        return superblock.getBlockSize() * (superblock.getTotalBlocks() - superblock.getFreeBlocks());
    }

    public boolean isV5() {
        return superblock.getVersion() == 5;
    }

    /**
     * Gets the valume name.
     */
    @Override
    public String getVolumeName() {
        return superblock.getName();
    }

    /**
     * @see org.jnode.fs.spi.AbstractFileSystem#createFile(FSEntry entry)
     */
    @Override
    protected FSFile createFile(FSEntry entry) throws IOException {
        return new XfsFile((XfsEntry) entry);
    }

    /**
     * @see org.jnode.fs.spi.AbstractFileSystem#createDirectory(FSEntry entry)
     */
    @Override
    protected FSDirectory createDirectory(FSEntry entry) throws IOException {
        return new XfsDirectory((XfsEntry) entry);
    }

    /**
     * @see org.jnode.fs.spi.AbstractFileSystem#createRootEntry()
     */
    @Override
    protected XfsEntry createRootEntry() throws IOException {
        long rootIno = superblock.getRootInode();
        return new XfsEntry(this.getINode(rootIno), "/", 0, this, null);
    }

    /**
     * Reads block from the file system.
     *
     * @param startBlock the start block.
     * @param dest       the destination to read into.
     * @throws IOException if an error occurs.
     */
    public void readBlocks(long startBlock, ByteBuffer dest) throws IOException {
        readBlocks(startBlock, 0, dest);
    }

    /**
     * Reads block from the file system.
     *
     * @param startBlock  the start block.
     * @param blockOffset the offset within the block to start reading from.
     * @param dest        the destination to read into.
     * @throws IOException if an error occurs.
     */
    public void readBlocks(long startBlock, int blockOffset, ByteBuffer dest) throws IOException {
        getApi().read(superblock.getBlockSize() * startBlock + blockOffset, dest);
    }

    /**
     * Gets the superblock.
     *
     * @return the superblock.
     */
    public Superblock getSuperblock() {
        return superblock;
    }

    /**
     * Gets the {@link AllocationGroupINode}.
     *
     * @return the {@link AllocationGroupINode}.
     */
    public AllocationGroupINode getAgINode() {
        return agINode;
    }

    /**
     * Gets the allocation group size.
     *
     * @return the allocation group size.
     */
    public long getAllocationGroupSize() {
        return allocationGroupSize;
    }
}

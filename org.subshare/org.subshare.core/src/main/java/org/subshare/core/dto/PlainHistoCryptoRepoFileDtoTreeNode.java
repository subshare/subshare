package org.subshare.core.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.util.AssertUtil;

public class PlainHistoCryptoRepoFileDtoTreeNode implements Iterable<PlainHistoCryptoRepoFileDtoTreeNode> {

	/**
	 * Create a single tree from the given {@code plainHistoCryptoRepoFileDtos}.
	 * <p>
	 * The given {@code plainHistoCryptoRepoFileDtos} must meet the following criteria:
	 * <ul>
	 * <li>It must not be <code>null</code>.
	 * <li>It may be empty.
	 * <li>If it is <i>not</i> empty, it may contain any number of elements, but:
	 * <ul>
	 * <li>It must contain exactly one root-node (with
	 * {@link RepoFileDto#getParentEntityID() RepoFileDto.parentEntityID} being <code>null</code>).
	 * <li>It must resolve completely, i.e. there must be a {@code RepoFileDto} for every
	 * referenced {@code parentEntityID}.
	 * </ul>
	 * </ul>
	 * @param plainHistoCryptoRepoFileDtos the Dtos to be organized in a tree structure. Must not be <code>null</code>. If
	 * empty, the method result will be <code>null</code>.
	 * @return the tree's root node. <code>null</code>, if {@code plainHistoCryptoRepoFileDtos} is empty.
	 * Never <code>null</code>, if {@code plainHistoCryptoRepoFileDtos} contains at least one element.
	 * @throws IllegalArgumentException if the given {@code plainHistoCryptoRepoFileDtos} does not meet the criteria stated above.
	 */
	public static PlainHistoCryptoRepoFileDtoTreeNode createTree(final Collection<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos) throws IllegalArgumentException {
		AssertUtil.assertNotNull("plainHistoCryptoRepoFileDtos", plainHistoCryptoRepoFileDtos);
		if (plainHistoCryptoRepoFileDtos.isEmpty())
			return null;

		final Map<Uid, PlainHistoCryptoRepoFileDtoTreeNode> id2RepoFileDtoTreeNode = new HashMap<>();
		for (final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto : plainHistoCryptoRepoFileDtos) {
			id2RepoFileDtoTreeNode.put(plainHistoCryptoRepoFileDto.getCryptoRepoFileId(), new PlainHistoCryptoRepoFileDtoTreeNode(plainHistoCryptoRepoFileDto));
		}

		PlainHistoCryptoRepoFileDtoTreeNode rootNode = null;
		for (final PlainHistoCryptoRepoFileDtoTreeNode node : id2RepoFileDtoTreeNode.values()) {
			final Uid parentId = node.getPlainHistoCryptoRepoFileDto().getParentCryptoRepoFileId();
			if (parentId == null) {
				if (rootNode != null)
					throw new IllegalArgumentException("Multiple root nodes!");

				rootNode = node;
			}
			else {
				final PlainHistoCryptoRepoFileDtoTreeNode parentNode = id2RepoFileDtoTreeNode.get(parentId);
				if (parentNode == null)
					throw new IllegalArgumentException("parentEntityID unknown: " + parentId);

				parentNode.addChild(node);
			}
		}

		if (rootNode == null)
			throw new IllegalArgumentException("There is no root node!");

		return rootNode;
	}

	private static final Comparator<PlainHistoCryptoRepoFileDtoTreeNode> nodeComparatorByNameOnly = new Comparator<PlainHistoCryptoRepoFileDtoTreeNode>() {
		@Override
		public int compare(PlainHistoCryptoRepoFileDtoTreeNode node0, PlainHistoCryptoRepoFileDtoTreeNode node1) {
			final String name0 = node0.getPlainHistoCryptoRepoFileDto().getRepoFileDto().getName();
			final String name1 = node1.getPlainHistoCryptoRepoFileDto().getRepoFileDto().getName();
			return name0.compareTo(name1);
		}
	};

	private PlainHistoCryptoRepoFileDtoTreeNode parent;
	private final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto;
	private final SortedSet<PlainHistoCryptoRepoFileDtoTreeNode> children = new TreeSet<PlainHistoCryptoRepoFileDtoTreeNode>(nodeComparatorByNameOnly);
	private List<PlainHistoCryptoRepoFileDtoTreeNode> flattenedTreeList;

	protected PlainHistoCryptoRepoFileDtoTreeNode(final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto) {
		this.plainHistoCryptoRepoFileDto = AssertUtil.assertNotNull("plainHistoCryptoRepoFileDto", plainHistoCryptoRepoFileDto);
	}

	public PlainHistoCryptoRepoFileDto getPlainHistoCryptoRepoFileDto() {
		return plainHistoCryptoRepoFileDto;
	}

	public PlainHistoCryptoRepoFileDtoTreeNode getParent() {
		return parent;
	}
	protected void setParent(final PlainHistoCryptoRepoFileDtoTreeNode parent) {
		this.parent = parent;
	}

	public Set<PlainHistoCryptoRepoFileDtoTreeNode> getChildren() {
		return Collections.unmodifiableSet(children);
	}

	protected void addChild(final PlainHistoCryptoRepoFileDtoTreeNode child) {
		child.setParent(this);
		children.add(child);
	}

	/**
	 * Gets the path from the root to the current node.
	 * <p>
	 * The path's elements are separated by a slash ("/").
	 * @return the path from the root to the current node. Never <code>null</code>.
	 */
	public String getPath() {
		final PlainHistoCryptoRepoFileDtoTreeNode parent = getParent();
		if (parent == null)
			return getPlainHistoCryptoRepoFileDto().getRepoFileDto().getName();
		else
			return parent.getPath() + '/' + getPlainHistoCryptoRepoFileDto().getRepoFileDto().getName();
	}

	public List<PlainHistoCryptoRepoFileDtoTreeNode> getLeafs() {
		final List<PlainHistoCryptoRepoFileDtoTreeNode> leafs = new ArrayList<PlainHistoCryptoRepoFileDtoTreeNode>();
		populateLeafs(this, leafs);
		return leafs;
	}

	private void populateLeafs(final PlainHistoCryptoRepoFileDtoTreeNode node, final List<PlainHistoCryptoRepoFileDtoTreeNode> leafs) {
		if (node.getChildren().isEmpty()) {
			leafs.add(node);
		}
		for (final PlainHistoCryptoRepoFileDtoTreeNode child : node.getChildren()) {
			populateLeafs(child, leafs);
		}
	}

	@Override
	public Iterator<PlainHistoCryptoRepoFileDtoTreeNode> iterator() {
		return getFlattenedTreeList().iterator();
	}

	public int size() {
		return getFlattenedTreeList().size();
	}

	private List<PlainHistoCryptoRepoFileDtoTreeNode> getFlattenedTreeList() {
		if (flattenedTreeList == null) {
			final List<PlainHistoCryptoRepoFileDtoTreeNode> list = new ArrayList<PlainHistoCryptoRepoFileDtoTreeNode>();
			flattenTree(list, this);
			flattenedTreeList = list;
		}
		return flattenedTreeList;
	}

	private void flattenTree(final List<PlainHistoCryptoRepoFileDtoTreeNode> result, final PlainHistoCryptoRepoFileDtoTreeNode node) {
		result.add(node);
		for (final PlainHistoCryptoRepoFileDtoTreeNode child : node.getChildren()) {
			flattenTree(result, child);
		}
	}

	public PlainHistoCryptoRepoFileDtoTreeNode getRoot() {
		final PlainHistoCryptoRepoFileDtoTreeNode parent = getParent();
		if (parent == null)
			return this;
		else
			return parent.getRoot();
	}
}

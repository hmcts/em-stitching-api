package uk.gov.hmcts.reform.em.stitching.pdf;

import java.util.LinkedList;
import java.util.List;

public class TreeNode<T> {

    private T data;
    private TreeNode<T> parent;
    private List<TreeNode<T>> children;

    public TreeNode(T data) {
        this.data = data;
        this.children = new LinkedList();
    }

    public T getParentData() {
        return this.parent.data;
    }

    public TreeNode<T> addChild(T child) {
        TreeNode<T> childNode = new TreeNode(child);
        childNode.parent = this;
        this.children.add(childNode);
        return childNode;
    }

    public TreeNode<T> findTreeNode(Comparable<T> cmp, TreeNode<T> startNode) {
        if (cmp.compareTo(startNode.data) == 0) {
            return this;
        }

        for (TreeNode<T> element : startNode.children) {
            T elData = element.data;
            if (cmp.compareTo(elData) == 0) {
                return element;
            } else {
                var find = findTreeNode(cmp, element);
                if (find != null) {
                    return find;
                }
            }
        }

        return null;
    }
}

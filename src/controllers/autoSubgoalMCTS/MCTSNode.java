package controllers.autoSubgoalMCTS;

import java.util.ArrayList;

public class MCTSNode<Data>
{
    public interface INodeVisitor<Data>
    {
        void visit(MCTSNode<Data> node);
    }

    MCTSNode<Data> parent;
    ArrayList<MCTSNode<Data>> children;
    Data data;

    boolean fullyExplored;
    double score;
    double lowerBound = Double.POSITIVE_INFINITY;
    double upperBound = Double.NEGATIVE_INFINITY;
    int visitCount;

    public MCTSNode(Data data)
    {
        this.children = new ArrayList<>();
        this.data = data;
    }

    public MCTSNode<Data> selectUCT(double explorationRate)
    {
        double highestUCT = Double.NEGATIVE_INFINITY;
        MCTSNode<Data> bestChild = null;
        for (MCTSNode<Data> child : children)
        {
            if (child.fullyExplored)
                continue;
            if (child.visitCount == 0)
                return child;

            double uct = (child.score - lowerBound) / (upperBound - lowerBound + 1);
            uct += explorationRate * Math.sqrt(Math.log(visitCount) / child.visitCount);

            if (uct > highestUCT)
            {
                highestUCT = uct;
                bestChild = child;
            }
        }

        return bestChild;
    }

    public MCTSNode<Data> addChild(Data childData)
    {
        MCTSNode<Data> newChild = new MCTSNode<Data>(childData);
        newChild.parent = this;
        children.add(newChild);
        return newChild;
    }

    public MCTSNode<Data> detachChild(int index)
    {
        MCTSNode<Data> child = children.remove(index);
        child.parent = null;
        return child;
    }

    public void backpropagate(double score)
    {
        MCTSNode<Data> currNode = this;
        while (currNode != null)
        {
            ++currNode.visitCount;
            currNode.score += (score - currNode.score) / currNode.visitCount;
            currNode.lowerBound = Math.min(score, currNode.lowerBound);
            currNode.upperBound = Math.max(score, currNode.upperBound);
            currNode = currNode.parent;
        }
    }

    public void backpropagate(double score, INodeVisitor<Data> visitor)
    {
        MCTSNode<Data> currNode = this;
        while (currNode != null)
        {
            ++currNode.visitCount;
            currNode.score += (score - currNode.score) / currNode.visitCount;
            currNode.lowerBound = Math.min(score, currNode.lowerBound);
            currNode.upperBound = Math.max(score, currNode.upperBound);
            visitor.visit(currNode);
            currNode = currNode.parent;
        }
    }

    public void setFullyExplored()
    {
        fullyExplored = true;

        MCTSNode<Data> currNode = this.parent;
        whileLoop: while(currNode != null)
        {
            for(MCTSNode<Data> child : currNode.children)
            {
                if(!child.fullyExplored)
                {
                    break whileLoop;
                }
            }

            currNode.fullyExplored = true;
            currNode = currNode.parent;
        }
    }

    public boolean isLeafNode() { return children.isEmpty(); }
    public boolean isRootNode() { return parent == null; }
}
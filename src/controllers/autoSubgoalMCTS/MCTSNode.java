package controllers.autoSubgoalMCTS;

import java.util.ArrayList;
import java.util.Random;

public class MCTSNode<Data>
{
    public interface INodeVisitor<Data>
    {
        void visit(MCTSNode<Data> node);
    }

    public MCTSNode<Data> parent;
    public ArrayList<MCTSNode<Data>> children;
    public Data data;

    public boolean fullyExplored;
    public double score;
    public double lowerBound = Double.POSITIVE_INFINITY;
    public double upperBound = Double.NEGATIVE_INFINITY;
    public int visitCount;

    public MCTSNode(Data data)
    {
        this.children = new ArrayList<>();
        this.data = data;
    }

    public MCTSNode<Data> selectUCT(double explorationRate, Random rng)
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
            uct += rng.nextDouble() * 1e-8; // Resolve ties randomly

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
        MCTSNode<Data> newChild = new MCTSNode<>(childData);
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

    public MCTSNode<Data> getChildWithHighestReturn()
    {
        double bestScore = Double.NEGATIVE_INFINITY;
        MCTSNode<Data> bestChild = null;
        for(MCTSNode<Data> child : children)
        {
            if(child.score > bestScore)
            {
                bestScore = child.score;
                bestChild = child;
            }
        }

        return bestChild;
    }

    public MCTSNode<Data> getChildWithHighestVisits()
    {
        double bestScore = Double.NEGATIVE_INFINITY;
        MCTSNode<Data> bestChild = null;
        for(MCTSNode<Data> child : children)
        {
            if(child.visitCount > bestScore)
            {
                bestScore = child.visitCount;
                bestChild = child;
            }
        }

        return bestChild;
    }

    public boolean isLeafNode() { return children.isEmpty(); }
    public boolean isRootNode() { return parent == null; }
}

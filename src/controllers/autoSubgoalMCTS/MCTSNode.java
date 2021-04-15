package controllers.autoSubgoalMCTS;

import java.util.ArrayList;

public class MCTSNode<Data>
{
    MCTSNode<Data> parent;
    ArrayList<MCTSNode<Data>> children;
    Data data;

    boolean fullyExplored;
    double score;
    double lowerBound = Double.MAX_VALUE;
    double upperBound = Double.MIN_VALUE;
    int visitCount;

    public MCTSNode(Data data)
    {
        this.children = new ArrayList<>();
        this.data = data;
    }

    public MCTSNode<Data> selectUCT(double explorationRate)
    {
        double highestUCT = Double.MIN_VALUE;
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

    public void addChild(Data childData)
    {
        MCTSNode<Data> newChild = new MCTSNode<Data>(childData);
        newChild.parent = this;
        children.add(newChild);
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

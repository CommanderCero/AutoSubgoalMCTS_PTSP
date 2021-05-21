package controllers.autoSubgoalMCTS.SubgoalPredicates;

import framework.core.Game;
import framework.utils.Vector2d;

import java.awt.*;

public class PositionGridPredicate implements ISubgoalPredicate
{
    public PositionGridPredicate(double cellSize, double epsilon)
    {
        this.cellSize = cellSize;
        this.epsilon = epsilon;
        this.epsilonSqrd = this.epsilon * this.epsilon;
        this.origin = new Vector2d(epsilon, epsilon);
    }

    @Override
    public boolean isSubgoal(Game state)
    {
        if(state.isEnded())
            return true;

        Vector2i cell = getCell(state.getShip().s);
        double offsetX = (state.getShip().s.x - origin.x) - cell.x * cellSize;
        double offsetY = (state.getShip().s.y - origin.y) - cell.y * cellSize;

        return (offsetX * offsetX + offsetY * offsetY) <= epsilonSqrd;
    }

    @Override
    public boolean isSameSubgoal(Game state1, Game state2)
    {
        if(state1.isEnded())
        {
            if(state2.isEnded())
            {
                return state1.getShip().s.sqDist(state2.getShip().s) <= epsilonSqrd;
            }

            return false;
        }

        Vector2i cell1 = getCell(state1.getShip().s);
        Vector2i cell2 = getCell(state2.getShip().s);
        return cell1.equals(cell2);
    }

    @Override
    public void render(Graphics2D graphics, Game state)
    {
        Vector2i cell = getCell(state.getShip().s);
        int xSteps = (int)(state.getMapSize().width / cellSize) + 1;
        int ySteps = (int)(state.getMapSize().height / cellSize) + 1;
        for(int x = 0; x < xSteps; x++)
        {
            for(int y = 0; y < ySteps; y++)
            {
                int worldX = (int)(origin.x + x * cellSize);
                int worldY = (int)(origin.y + y * cellSize);

                graphics.setColor(Color.RED);
                graphics.fillOval(worldX - 1, worldY - 1, 2, 2);
                if(cell.x == x && cell.y == y && isSubgoal(state))
                {
                    graphics.setColor(Color.YELLOW);
                    if(isSubgoal(state))
                    {
                        graphics.setColor(Color.YELLOW);
                    }
                }
                else
                {
                    graphics.setColor(Color.GRAY);
                }
                graphics.drawOval((int)(worldX - epsilon), (int)(worldY - epsilon), (int)epsilon * 2, (int)epsilon * 2);
            }
        }
    }

    private Vector2i getCell(Vector2d pos)
    {
        return new Vector2i(Math.round((pos.x - origin.x) / cellSize), Math.round((pos.y - origin.x) / cellSize));
    }

    private Vector2d origin;
    private double cellSize;
    private double epsilon;
    private double epsilonSqrd;
}

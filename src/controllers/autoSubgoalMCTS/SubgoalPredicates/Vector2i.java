package controllers.autoSubgoalMCTS.SubgoalPredicates;

public class Vector2i
{
    public Vector2i()
    {
        x = 0;
        y = 0;
    }

    public Vector2i(long x, long y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj.getClass() != getClass())
            return false;

        Vector2i other = (Vector2i)obj;
        return other.x == x && other.y == y;
    }

    public long x;
    public long y;
}

package controllers.mcts;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 03/04/12
 * Time: 13:44
 * To change this template use File | Settings | File Templates.
 */
public class ARCCOS
{
    public static final double _PI = Math.PI;
    public static final double _5PI6 = 5*Math.PI / 6;
    public static final double _3PI4 = 3*Math.PI / 4;
    public static final double _2PI3 = 2*Math.PI / 3;
    public static final double _PI2 = Math.PI / 2;
    public static final double _PI3 = Math.PI / 3;
    public static final double _PI4 = Math.PI / 4;
    public static final double _PI6 = Math.PI / 6;
    public static final double _CERO = 0;

    public static double getArcos(double dot)
    {
        if(dot == -1) return _PI;
        else if(dot < -0.85) return _5PI6;
        else if(dot < -0.7) return _3PI4;
        else if(dot < -0.5) return _2PI3;
        else if(dot < 0) return _PI2;
        else if(dot < 0.5) return _PI3;
        else if(dot < 0.7) return _PI4;
        else if(dot < 0.85) return _PI6;
        else return _CERO; //dot == 1
    }
}

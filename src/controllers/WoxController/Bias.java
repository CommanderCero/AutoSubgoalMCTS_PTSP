package controllers.WoxController;

/**
 * Created by IntelliJ IDEA.
 * User: diego
 * Date: 21/03/12
 * Time: 00:12
 * This is a class that contains three booleans, which determine the probability of choosing one action or another.
 * It is shown as an example of a class that can be serialized and deserialized using WOX.
 */
public class Bias 
{
    /**
     * Probability of selecting a thrust action over a non thrust action.
     */
    private double m_selectThrust;

    /**
     *  Probability of selecting a straight action over a turning action
     */
    private double m_selectStraight;

    /**
     *  When a turning action is selected, probability of selecting turning right over left.
     */
    private double m_selectRight;


    /**
     * Gets the probability to thrust.
     */
    public double getThrust() {return m_selectThrust;}

    /**
     * Gets the probability to go straight and not turn.
     */
    public double getStraight() {return m_selectStraight;}

    /**
     * Gets the probability to go right and not turn.
     */
    public double getRight() {return m_selectRight;}


}

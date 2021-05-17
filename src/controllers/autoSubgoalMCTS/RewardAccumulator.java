package controllers.autoSubgoalMCTS;

public class RewardAccumulator
{
    private double rewardSum;
    private double multiplier;

    public double rewardDecay;

    public RewardAccumulator(double rewardDecay)
    {
        rewardSum = 0;
        multiplier = 1;
        this.rewardDecay = rewardDecay;
    }

    public void addReward(double reward)
    {
        rewardSum += multiplier * reward;
        multiplier *= rewardDecay;
    }

    public void reset()
    {
        rewardSum = 0;
        multiplier = 1;
    }

    public double getRewardSum()
    {
        return rewardSum;
    }
}

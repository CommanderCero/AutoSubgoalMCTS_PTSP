package controllers.autoSubgoalMCTS;

public class RewardAccumulator
{
    private double rewardSum;
    private double multiplier;

    public double rewardDecay;

    RewardAccumulator(double rewardDecay)
    {
        rewardSum = 0;
        multiplier = 1;
        this.rewardDecay = rewardDecay;
    }

    void addReward(double reward)
    {
        rewardSum += multiplier * reward;
        multiplier *= rewardDecay;
    }

    void reset()
    {
        rewardSum = 0;
        multiplier = 1;
    }

	double getRewardSum()
    {
        return rewardSum;
    }
}

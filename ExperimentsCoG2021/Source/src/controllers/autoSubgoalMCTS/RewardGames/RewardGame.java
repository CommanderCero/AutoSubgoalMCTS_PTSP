package controllers.autoSubgoalMCTS.RewardGames;

import framework.core.Game;

public abstract class RewardGame
{
    public RewardGame(Game initialState)
    {
        this();
        currentState = initialState;
    }

    protected RewardGame()
    {
        currentTickRewardSum = 0;
        rewardSum = 0;
    }

    // Returns the reward for each action, useful for computing a decaying reward sum
    public double tick(int action)
    {
        callCounter++;
        currentTickRewardSum = 0;
        tickInternal(currentState, action);
        rewardSum += currentTickRewardSum;
        return currentTickRewardSum;
    }

    // Evaluates how good the current state is
    // This function serves to support returning only one value at the end of a trajectory.
    // In contrast to tick() which returns a bunch of rewards
    public abstract double evaluate();

    public RewardGame getCopy()
    {
        RewardGame copy = copyInternal();
        copy.rewardSum = rewardSum;
        copy.currentState = currentState.getCopy();
        return copy;
    }

    public boolean isEnded() {return currentState.isEnded();}
    public double getRewardSum() { return rewardSum; }
    public Game getState() { return currentState; }
    public void setState(Game newState) { currentState = newState; }
    public static int getCalls() {return callCounter;}
    public static void resetCalls() {callCounter = 0;}

    protected abstract void tickInternal(Game state, int action);
    protected abstract RewardGame copyInternal();

    protected void addReward(double reward)
    {
        currentTickRewardSum += reward;
    }

    private Game currentState;
    private double rewardSum;
    private double currentTickRewardSum;
    private static int callCounter;
}

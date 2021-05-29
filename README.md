# AutoSubgoalMCTS_PTSP
This repository implements a Quality Diversity - Monte Carlo Tree Search algorithm. In general, we replace the expansion step from a traditional MCTS algorithm with a quality diversity search for finding novel and rewarding macro actions. The new expansion step allows QD-MCTS to prune many trajectories while still ensuring that it can sufficiently explore the environment.

**Note** We use a copy of the framework provided by the [Physical Travelling Salesmen competition](http://diego-perez.net/papers/PtspCompetition2012.pdf) to test the agent.

## Structure

<dl>
  <dt><strong>maps</strong></dt>
  <dd>Contains the definition and a picture of all maps provided by the PTSP competition.</dd>
  <dt><strong>src</strong></dt>
  <dd>Contains the source code of the framework, including the implementation of various agents.</dd>
  <dt><strong>ExperimentsCoG2021</strong></dt>
  <dd>Contains the results of the experiments for a paper submitted to CoG 2021. For more details, see the corresponding subsection below.</dd>
</dl>

You can find the QD-MCTS algorithm under "src/controllers/autoSubgoalMCTS". Note that this subfolder contains various agents. **MyMCTSController** implements the vanilla MCTS algorithm with tree reuse. The **AutoSubgoalController** implements both Subgoal MCTS and QD-MCTS. This controller implements a Subgoal MCTS algorithm that runs a subgoal search in each leaf node. The used subgoal search essentially decides if it's a vanilla Subgoal MCTS algorithm or QD-MCTS. The different subgoal searches are defined in the folder "SubgoalSearch".

## CoG 2021
The folder **ExperimentsCoG2021**  contains subfolders with all the results of the tested algorithms. Each subfolder contains a .csv containing data like waypoints visited or how many steps were taken. Additionally, the subfolder contains a bunch of textfiles representing the trajectory for each trial.

To ensure that the experiments are reproducible, we have copied the source code into the experiments folder. Meaning **if you want to reproduce the results**, use the project defined in "ExperimentsCoG2021/Source". Run "src/framework/Experiments" to reproduce the results. Execute "src/framework/ExecFromData" to see how a saved trajectory looks like. Lastly, you can execute "src/ExecSync" to run any agent with GUI enabled. **Important** Please read the comments in all of these files to ensure that you execute them correctly.
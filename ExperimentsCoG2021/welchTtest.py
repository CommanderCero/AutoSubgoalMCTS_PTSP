import numpy as np
import pandas as pd
from scipy import stats
import collections
import os

def getMapName(path):
    rawName = os.path.splitext(os.path.basename(path))[0]
    # We now have ptsp_map$ID$, remove the ptsp_
    return rawName.split("_")[1]

if __name__ == "__main__":
    # Load experiments to display
    experiments = {
        "Vanilla MCTS": "./VanillaMCTS/VanillaMCTS_2021_05_25_15_51.csv",
        "S-MCTS": "./S-MCTS/S-MCTS_2021_05_26_12_58.csv",
        "MS-MCTS": "./MS-MCTS/MS-Subgoal-MCTS_2021_05_26_10_51.csv",
        "QD-S-MCTS": "./QD-S-MCTS/QD-Subgoal-MCTS_2021_05_25_18_18.csv",
        "Vanilla GA": "./VanillaGA/VanillaGA_2021_06_03_14_48.csv",
    }
    
    # Prepare data
    mapToIndex = None
    waypointSamples = {}
    stepsPerWaypointSamples = {}
    for key in experiments:
        data = pd.read_csv(experiments[key], delimiter="\t")
        # Shorten map name to only contain contain "map$id$"
        data["MapName"] = [getMapName(name) for name in data["MapName"]]
        
        # Create a df containing the map on the y-axis and the trials on the x-axis
        x = data.groupby('MapName')['WaypointsVisited'] \
              .apply(lambda x: pd.DataFrame(x.values.tolist(), columns=['WaypointsVisited'])) \
              .unstack()
        waypointSamples[key] = x

        data["StepsPerWaypoint"] = data["Steps"] / (data["WaypointsVisited"] + 1)        
        # Create a df containing the map on the y-axis and the trials on the x-axis
        x = data.groupby('MapName')['StepsPerWaypoint'] \
              .apply(lambda x: pd.DataFrame(x.values.tolist(), columns=['StepsPerWaypoint'])) \
              .unstack()
        stepsPerWaypointSamples[key] = x
    
    
    # Test significance if QD-S-MCTS visited more waypoints
    for alg in ["Vanilla MCTS", "S-MCTS", "MS-MCTS", "Vanilla GA"]:
        results = stats.ttest_ind(waypointSamples["QD-S-MCTS"], waypointSamples[alg],
                                  alternative="greater",
                                  equal_var=False,
                                  axis=0)
        
        # Output results
        print(f"Result for Ttest that QD-S-MCTS visited more waypoints than {alg}")
        for i, mapName in enumerate(waypointSamples["QD-S-MCTS"].index):
            print(f"{mapName}: statistics={results.statistic[i]:.4f}\tp-value={results.pvalue[i]:.4f}")
            
        print(f"Average p-value: {np.mean(results.pvalue):.4f}")
        print()
        
    # Test significance if QD-S-MCTS needed less steps on average to reach a waypoint than other algorithms
    for alg in ["Vanilla MCTS", "S-MCTS", "MS-MCTS", "Vanilla GA"]:
        results = stats.ttest_ind(stepsPerWaypointSamples["QD-S-MCTS"], stepsPerWaypointSamples[alg],
                                  alternative="less",
                                  equal_var=False,
                                  axis=0)
        
        # Output results
        print(f"Result for Ttest that QD-S-MCTS needed less steps to reach a waypoint than {alg}")
        for i, mapName in enumerate(stepsPerWaypointSamples["QD-S-MCTS"].index):
            print(f"{mapName}: statistics={results.statistic[i]:.4f}\tp-value={results.pvalue[i]:.4f}")
            
        print(f"Average p-value: {np.mean(results.pvalue):.4f}")
        print()
            
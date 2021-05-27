import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import os
import seaborn as sns

def getMapName(path):
    rawName = os.path.splitext(os.path.basename(path))[0]
    # We now have ptsp_map$ID$, remove the ptsp_
    return rawName.split("_")[1]

if __name__ == "__main__":
    # Load experiments to display
    experiments = {
        "Vanilla MCTS": {"filePath": "./VanillaMCTS/VanillaMCTS_2021_05_25_15_51.csv", "color": "blue"},
        "S-MCTS": {"filePath": "./S-MCTS/S-MCTS_2021_05_26_12_58.csv", "color": "red"},
        "MS-MCTS": {"filePath": "./MS-MCTS/MS-Subgoal-MCTS_2021_05_26_10_51.csv", "color": "red"},
        "QD-S-MCTS": {"filePath": "./QD-S-MCTS/QD-Subgoal-MCTS_2021_05_25_18_18.csv", "color": "red"},
    }
    
    avgWaypoints = {}
    avgStepsPerWaypoint = {}
    for key in experiments:
        data = pd.read_csv(experiments[key]["filePath"], delimiter="\t")
        # Shorten map name to only contain contain "map$id$"
        data["MapName"] = [getMapName(name) for name in data["MapName"]]
        
        data["StepsPerWaypoint"] = data["Steps"] / (data["WaypointsVisited"] + 1)
        mapAvg = data.groupby("MapName").mean()
        
        avgWaypoints[key] = mapAvg["WaypointsVisited"]
        avgStepsPerWaypoint[key] = mapAvg["StepsPerWaypoint"]
    
    # Create plot for avg steps per waypoint
    avgWaypointDf = pd.DataFrame(avgWaypoints).reset_index()
    flatDf = avgWaypointDf.melt(id_vars="MapName")
    g = sns.catplot(kind="bar", x="value", y="MapName", hue="variable", data=flatDf, legend=False)
    g.despine(right=True)
    for ax in g.axes.flat:
        ax.grid(True, axis='x')
        ax.set_axisbelow(True)
    plt.legend(loc="lower center", bbox_to_anchor=(0.5, -0.18), ncol=len(avgWaypointDf.columns), frameon=False)
    plt.xticks(range(0, 11))
    plt.ylabel("")
    plt.xlabel("Avg. Waypoints Visited")
    g.savefig("stepsPerWaypoint.png", bbox_inches='tight')
    
    #avgStepDf = pd.DataFrame(avgStepsPerWaypoint)
    #avgStepDf.plot.bar()
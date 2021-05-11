import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import os

def lighten_color(color, amount=0.5):
    """
    Lightens the given color by multiplying (1-luminosity) by the given amount.
    Input can be matplotlib color string, hex string, or RGB tuple.

    Examples:
    >> lighten_color('g', 0.3)
    >> lighten_color('#F034A3', 0.6)
    >> lighten_color((.3,.55,.1), 0.5)
    """
    import matplotlib.colors as mc
    import colorsys
    try:
        c = mc.cnames[color]
    except:
        c = color
    c = colorsys.rgb_to_hls(*mc.to_rgb(c))
    return colorsys.hls_to_rgb(c[0], 1 - amount * (1 - c[1]), c[2])

# ToDo Stupid name, wtf
def plotStatistics(data, columnName, label, color='black'):
    mapData = data.groupby("MapName")
    mapNames = [os.path.splitext(os.path.basename(key))[0] for key, _ in mapData]
    
    mins = mapData[columnName].min()
    maxes = mapData[columnName].max()
    means = mapData[columnName].mean()
    std = mapData[columnName].std()
    
    # create stacked errorbars:
    plt.errorbar(means, mapNames, xerr=[means - mins, maxes - means], fmt=' ', ecolor=lighten_color(color), lw=1)
    plt.errorbar(means, mapNames, xerr=std, fmt='o', lw=3, label=label, ecolor=color, c=color)

if __name__ == "__main__":
    # Load experiments to display
    experiments = {
        "MCTS": {"filePath": "./MCTS_2021_05_03_12_43.csv", "color": "blue"},
        "S-MCTS": {"filePath": "./AutoSubgoalMCTS_2021_05_04_13_20.csv", "color": "red"},
        "Naive MCTS": {"filePath": "./NaiveMCTS_2021_05_07_11_15.csv", "color": "green"},
    }
    for key in experiments:
        experiments[key]["data"] = pd.read_csv(experiments[key]["filePath"], delimiter="\t")
    
    # Plot steps per waypoint
    f = plt.figure()
    for experimentName in experiments:
        df = experiments[experimentName]["data"]
        color = experiments[experimentName]["color"]
        
        df["Steps"] = df["Steps"] / (df["WaypointsVisited"] + 1) # Normalize to steps per waypoint
        plotStatistics(df, "Steps", experimentName, color)
    plt.xlim(0, 1000)
    plt.xlabel("Avg. Steps per Waypoint")
    plt.legend(loc="upper right", bbox_to_anchor=(1.15,1.15))
    plt.show()
    f.savefig("stepsPerWaypoint.png", bbox_inches='tight')
    
    # Plot waypoints visited
    f = plt.figure()
    for experimentName in experiments:
        df = experiments[experimentName]["data"]
        color = experiments[experimentName]["color"]
        plotStatistics(df, "WaypointsVisited", experimentName, color)
    plt.xlim(-1, 11)
    plt.xlabel("Waypoints visited")
    plt.legend(loc="upper right", bbox_to_anchor=(1.15,1.15))
    plt.show()
    f.savefig("waypointsVisited.png", bbox_inches='tight')
    
    
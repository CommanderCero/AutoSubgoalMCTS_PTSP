import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import os

if __name__ == "__main__":
    filePath = "./MCTS_2021_05_03_12_43.csv"
    data = pd.read_csv(filePath, delimiter="\t")
    mapData = data.groupby("MapName")
    mapNames = [os.path.splitext(os.path.basename(key))[0] for key, _ in mapData]
    
    # construct some data like what you have:
    x = np.random.randn(100, 8)
    mins = mapData["Steps"].min() / 10
    maxes = mapData["Steps"].max()  / 10
    means = mapData["Steps"].mean()  / 10
    std = mapData["Steps"].std()  / 10
    
    # create stacked errorbars:
    f = plt.figure()
    plt.errorbar(means, mapNames, xerr=std, fmt='ok', lw=3)
    plt.errorbar(means, mapNames, xerr=[means - mins, maxes - means], fmt='.k', ecolor='gray', lw=1)
    plt.show()
    f.savefig("myFig.pdf", bbox_inches='tight')
    
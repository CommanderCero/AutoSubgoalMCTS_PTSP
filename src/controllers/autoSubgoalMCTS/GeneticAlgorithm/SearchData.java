package controllers.autoSubgoalMCTS.GeneticAlgorithm;

import framework.utils.Vector2d;

import java.util.ArrayList;

public class SearchData
{
    public Vector2d[] trajectory;

    public SearchData(int trajLength)
    {
        trajectory = new Vector2d[trajLength];
    }
}

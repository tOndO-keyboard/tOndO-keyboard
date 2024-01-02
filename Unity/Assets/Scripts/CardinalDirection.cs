public enum CardinalDirection
{ 
    Null = -1, 
    Center, 
    East, 
    NorthEast, 
    North, 
    NorthWest, 
    West, 
    SouthWest, 
    South, 
    SouthEast 
};

public class CardinalDirectionSorter
{
    CardinalDirection starter;
    bool clockwise;

    public CardinalDirectionSorter(CardinalDirection starter, bool clockwise)
    {
        this.starter = starter;
        this.clockwise = clockwise;
    }

    public int CircularCompare(CardinalDirection valueA, CardinalDirection valueB)
    {
        if(valueA == valueB)
        {
            return 0;
        }

        if(valueA == CardinalDirection.Center)
        {
            return 1;
        }

        if(valueB == CardinalDirection.Center)
        {
            return -1;
        }

        if(valueA == CardinalDirection.Null)
        {
            return -1;
        }

        if(valueB == CardinalDirection.Null)
        {
            return 1;
        }

        int iStarter = (int)starter;
        int iValueA = (int)valueA;
        int iValueB = (int)valueB;

        if(!clockwise)
        {
            iValueA = iValueA - iStarter;
            if(iValueA < (int)CardinalDirection.Center)
            {
                iValueA = ((int)CardinalDirection.SouthEast + 1) + iValueA;
            }

            iValueB = iValueB - iStarter;
            if(iValueB < (int)CardinalDirection.Center)
            {
                iValueB = ((int)CardinalDirection.SouthEast + 1) + iValueB;
            }

            return iValueA.CompareTo(iValueB);
        }
        else
        {
            iValueA = iValueA + ((int)CardinalDirection.SouthEast - iStarter);
            if(iValueA > (int)CardinalDirection.SouthEast)
            {
                iValueA = iValueA - ((int)CardinalDirection.SouthEast + 1);
            }

            iValueB = iValueB + ((int)CardinalDirection.SouthEast - iStarter);
            if(iValueB > (int)CardinalDirection.SouthEast)
            {
                iValueB = iValueB - ((int)CardinalDirection.SouthEast + 1);
            }

            return iValueB.CompareTo(iValueA);
        }
    }
}
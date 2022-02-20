package nio.util;

public class Pair <F, S>
{
    final F firstItem;
    final S secondItem;

    public Pair (F firstItem, S secondItem)
    {
        this.firstItem = firstItem;
        this.secondItem = secondItem;
    }

    public static <L, R> Pair <L, R> of (L left, R right)
    {
        return new Pair <L, R> (left, right);
    }

    public F getFirstItem ()
    {
        return firstItem;
    }

    public S getSecondItem ()
    {
        return secondItem;
    }
}

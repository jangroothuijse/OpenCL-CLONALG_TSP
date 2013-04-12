package nl.ru.bio.helper;
/**
 * @authors Jan Groothuijse, Niklas Weber, Rob Tiemens
 */
public class Coordinate {
	public int x;
	public int y;
	
	public Coordinate(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public float Distance(Coordinate c)
	{
		return (float) Math.sqrt(Math.pow(this.x *  - c.x,2) +  Math.pow(this.y - c.y,2));
	}

}

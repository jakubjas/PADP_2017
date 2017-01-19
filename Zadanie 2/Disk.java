class Disk implements DiskInterface
{
	private int[] sectors;
	private int size;
	private boolean error = false;

	public Disk(int size)
	{
		this.size = size;
		sectors = new int[size];

		for (int i=0; i<size; i++)
		{
			sectors[i] = 0;
		}
	}

	public void write(int sector, int value) throws DiskError
	{
		if (error) 
		{
			throw new DiskError();
		}

		try
		{
			System.out.println("Ide spac... - WRITE (sektor: " + sector + ", wartosc: " + value + ")");
			Thread.sleep(1000);
			System.out.println("WSTALEM... - WRITE (sektor: " + sector + ", wartosc: " + value + ")");
		}
		catch(Exception e)
		{

		}

		sectors[sector] = value;
	}

	public int read(int sector) throws DiskError
	{
		if (error)
		{
			throw new DiskError();
		} 

		try
		{
			System.out.println("Ide spac... - READ (sektor: " + sector + ", wartosc: " + sectors[sector] + ")");
			Thread.sleep(10000);
			System.out.println("WSTALEM... - READ (sektor: " + sector + ", wartosc: " + sectors[sector] + ")");
		}
		catch(Exception e)
		{

		}

		return sectors[sector];
	}

	public void throwError()
	{
		error = true;
	}

	public int size()
	{
		return size;
	}
}
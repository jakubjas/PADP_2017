import java.util.concurrent.ConcurrentLinkedQueue;

class Transaction
{
	private int sector;
	private int value;

    Transaction(int sector, int value)
    {
        this.sector = sector;
        this.value = value;
    }

	int getSector()
	{
		return this.sector;
	}

	int getValue()
	{
		return this.value;
	}
}

class RAID2 implements RAIDInterface2 {

	private DiskInterface2[] RAID_Drive;
	private ConcurrentLinkedQueue<Transaction> queue = new ConcurrentLinkedQueue<>();
	private int RAID_Capacity;
	private boolean[] diskReady;
	private boolean RAID_On;
	private volatile int rwCounter;

	private void incRWCounter()
	{
		rwCounter++;
	}

	private void decRWCounter()
	{
		rwCounter--;
	}

	@Override
	public void addDisks(DiskInterface2[] array) {
		RAID_Drive = array;
		RAID_Capacity = (RAID_Drive.length-1)*RAID_Drive[0].size();
		diskReady = new boolean[RAID_Drive.length];
		rwCounter = 0;

		for (int i=0; i<diskReady.length; i++)
		{
			diskReady[i] = true;
		}

		RAID_On = true;
	}

	private void writeHelper(int sector, int value) {

		if (!RAID_On)
		{
			return;
		}

		int physical_disk = sector / RAID_Drive[0].size();
		int physical_sector = sector % RAID_Drive[0].size();

		incRWCounter();

		if (RAID_Drive[physical_disk] != null)
		{
			synchronized(RAID_Drive[physical_disk])
			{
				diskReady[physical_disk] = false;
				RAID_Drive[physical_disk].write(physical_sector,value);
				diskReady[physical_disk] = true;
				RAID_Drive[physical_disk].notifyAll();
			}

			writeCheckSumToDisk(physical_sector,calculateCheckSum(physical_sector,physical_disk,value));
		}

		decRWCounter();
	}

    private int readHelper(int sector)
    {
        if (!RAID_On)
        {
            return -1;
        }

        int physical_disk = sector / RAID_Drive[0].size();
        int physical_sector = sector % RAID_Drive[0].size();
        int retrievedData = 0;

        incRWCounter();

        if (RAID_Drive[physical_disk] != null)
        {
            synchronized(RAID_Drive[physical_disk])
            {
                while(!diskReady[physical_disk])
                {
                    try
                    {
                        RAID_Drive[physical_disk].wait();
                    }
                    catch(InterruptedException ie)
                    {
                        System.out.println(ie);
                    }
                }

                diskReady[physical_disk] = false;
                retrievedData = RAID_Drive[physical_disk].read(physical_sector);
                diskReady[physical_disk] = true;
                RAID_Drive[physical_disk].notifyAll();
            }

            decRWCounter();
        }
        else
        {
            decRWCounter();
            retrievedData = readUponDiskFailure(physical_sector);
        }

        return retrievedData;
    }

    @Override
    public void write(int sector, int value) {

        Transaction t = new Transaction(sector, value);
        queue.add(t);

        new Thread(new Runnable() { public void run() {
            writeHelper(sector, value);
            queue.remove(t);
        }}).start();


    }

	@Override
	public int read(int sector) {

        Transaction result = null;

        for(Transaction t : queue) {
            if(t.getSector() != sector) continue;

            result = t;
        }

        if(result!=null)
            return result.getValue();

		return readHelper(sector);
	}

	private int readUponDiskFailure(int sector)
	{
		int retrievedData = readCheckSum(sector);

		for (int i=0; i<RAID_Drive.length-1; i++)
		{
			if (RAID_Drive[i] != null)
			{
				synchronized(RAID_Drive[i])
				{
					while(!diskReady[i])
					{
						try
						{
							RAID_Drive[i].wait();
						}
						catch(InterruptedException ie)
						{
							System.out.println(ie);
						}
					}

					diskReady[i] = false;
					retrievedData -= RAID_Drive[i].read(sector);
					diskReady[i] = true;
					RAID_Drive[i].notifyAll();
				}
			}
		}

        return retrievedData;
	}

	private int calculateCheckSum(int sector, int disk, int value)
	{
		int checkSum = 0;

		for (int i=0; i<RAID_Drive.length-1; i++)
		{
			if (i == disk)
			{
				checkSum += value;
			}
			else if (RAID_Drive[i] != null)
			{
				synchronized(RAID_Drive[i])
				{
					while(!diskReady[i])
					{
						try
						{
							RAID_Drive[i].wait();
						}
						catch(InterruptedException ie)
						{
							System.out.println(ie);
						}
					}

					diskReady[i] = false;
					checkSum += RAID_Drive[i].read(sector);
					diskReady[i] = true;
					RAID_Drive[i].notifyAll();
				}
			}
		}

		return checkSum;
	}

	private void writeCheckSumToDisk(int sector, int checksum)
	{
        Transaction t = new Transaction(sector, checksum);
        queue.add(t);

		int backupDisk = RAID_Drive.length-1;

		if (RAID_Drive[backupDisk] != null)
		{
			synchronized(RAID_Drive[backupDisk])
			{
				diskReady[backupDisk] = false;
				RAID_Drive[backupDisk].write(sector,checksum);
				diskReady[backupDisk] = true;
				RAID_Drive[backupDisk].notifyAll();
			}
		}

        queue.remove(t);
	}

	private int readCheckSum(int sector)
	{
		int checkSum = 0;

		synchronized(RAID_Drive[RAID_Drive.length-1])
		{
			while(!diskReady[RAID_Drive.length-1])
			{
				try
				{
					RAID_Drive[RAID_Drive.length-1].wait();
				}
				catch(InterruptedException ie)
				{
					System.out.println(ie);
				}
			}

			diskReady[RAID_Drive.length-1] = false;
			checkSum = RAID_Drive[RAID_Drive.length-1].read(sector);
			diskReady[RAID_Drive.length-1] = true;
			RAID_Drive[RAID_Drive.length-1].notifyAll();
		}

		return checkSum;
	}

	@Override
	public int size()
	{
		if (!RAID_On)
		{
			return -1;
		}

		return RAID_Capacity;
	}

	@Override
	public void shutdown() {
		RAID_On = false;
		while (rwCounter != 0) {}
	}

	@Override
	public boolean sectorInUse(int sector) {

        for(Transaction t : queue) {
            if(t.getSector() == sector)
                return true;
        }

		return false;
	}
}

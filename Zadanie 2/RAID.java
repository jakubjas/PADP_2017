// Author: Jakub Jas

class RAID implements RAIDInterface {

	protected DiskInterface[] RAID_Drive;
			  int RAID_Capacity;
			  boolean[] diskError;
			  boolean[] diskReady;
			  boolean RAID_On;
			  volatile int rwCounter;

	private void incRWCounter()
	{
		rwCounter++;
	}

	private void decRWCounter()
	{
		rwCounter--;
	}

	public void addDisks(DiskInterface[] array)
	{
		RAID_Drive = array;
		RAID_Capacity = (RAID_Drive.length-1)*RAID_Drive[0].size();
		diskError = new boolean[RAID_Drive.length];
		diskReady = new boolean[RAID_Drive.length];
		rwCounter = 0;

		for (int i=0; i<diskError.length; i++)
		{
			diskError[i] = false;
		}

		for (int i=0; i<diskReady.length; i++)
		{
			diskReady[i] = true;
		}

		RAID_On = true;
	}

	public void write(int sector, int value)
	{ 
		if (!RAID_On)
		{
			return;
		}

		int physical_disk = sector / RAID_Drive[0].size();
		int physical_sector = sector % RAID_Drive[0].size();

		incRWCounter();

		try
		{
			if (!diskError[physical_disk])
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
		}
		catch (DiskInterface.DiskError e)
		{
			diskError[physical_disk] = true;
		}
		finally
		{
			decRWCounter();
		}
	}

	public int read(int sector)
	{
		if (!RAID_On)
		{
			return -1;
		}

		int physical_disk = sector / RAID_Drive[0].size();
		int physical_sector = sector % RAID_Drive[0].size();
		int retrievedData = 0;

		incRWCounter();

		if (!diskError[physical_disk])
		{
			try
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
				
			}
			catch (DiskInterface.DiskError e)
			{
				diskError[physical_disk] = true;
				retrievedData = readUponDiskFailure(physical_sector);
			}
			finally
			{
				decRWCounter();
			}
		}
		else
		{
			decRWCounter();
			retrievedData = readUponDiskFailure(physical_sector);
		}

		return retrievedData;
	}

	int readUponDiskFailure(int sector)
	{
		int retrievedData = readCheckSum(sector);

		try
		{
			for (int i=0; i<RAID_Drive.length-1; i++)
			{
				if (!diskError[i])
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
		catch (DiskInterface.DiskError e)
		{
			System.out.println(e);
			decRWCounter();
		}

		return -1;
	}

	int calculateCheckSum(int sector, int disk, int value)
	{
		int checkSum = 0;

		for (int i=0; i<RAID_Drive.length-1; i++)
		{
			if (i == disk)
			{
				checkSum += value;
			}
			else if (!diskError[i])
			{
		 		try
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
		 		catch (DiskInterface.DiskError e)
		 		{
		 			diskError[i] = true;
		 		}
		 	}
		}

		return checkSum;
	}

	void writeCheckSumToDisk(int sector, int checksum)
	{
		int backupDisk = RAID_Drive.length-1;

		try 
		{
			if (!diskError[backupDisk])
			{
				synchronized(RAID_Drive[backupDisk])
				{
					diskReady[backupDisk] = false;
					RAID_Drive[backupDisk].write(sector,checksum);
					diskReady[backupDisk] = true;
					RAID_Drive[backupDisk].notifyAll();
				}
			}
		}
		catch (DiskInterface.DiskError e)
		{
			diskError[backupDisk] = true;
		}
	}

	int readCheckSum(int sector)
	{
		int checkSum = 0;

		try 
		{
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
		}
		catch (DiskInterface.DiskError e)
		{
			System.out.println(e);
		}

		return checkSum;
	}

	public int size()
	{
		if (!RAID_On)
		{
			return -1;
		}

		return RAID_Capacity;
	}

	public void shutdown()
	{
		RAID_On = false;
		while (rwCounter != 0) {}
	}
}
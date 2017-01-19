/**
 * Created by Grzegorz Trela on 14.11.16.
 */

import static java.lang.Thread.sleep;

public class Start
{
	public static void main(String[] args)
	{
		RAID dysk = new RAID();
		class Test implements DiskInterface
		{

			volatile int licznik = 0;

			public Test(int wielkosc)
			{
				lista = new int[wielkosc];
			}

			int[] lista;

			@Override
			public void write (int sector, int value) throws DiskError
			{
				if (licznik == 3) throw new DiskInterface.DiskError();
				else
				{
					lista[sector] = value;
				}
			}

			@Override
			public int read(int sector) throws DiskError
			{
				if (licznik == 4) throw new DiskInterface.DiskError();
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				return lista[sector];

			}

			public void ustawError(int nr)
			{
				licznik = nr;
			}

			@Override
			public int size()
			{
				return lista.length;
			}
		}


		Test testuje = new Test(10);
		Test testuje1 = new Test(10);
		Test testuje2 = new Test(10);
		Test testuje3 = new Test(10);
		Test testuje4 = new Test(10);

		DiskInterface[] listadyskow = new DiskInterface[5];

		listadyskow[0] = testuje;
		listadyskow[1] = testuje1;
		listadyskow[2] = testuje2;
		listadyskow[3] = testuje3;
		listadyskow[4] = testuje4;

		dysk.addDisks(listadyskow);

		for (int i = 1; i < 40; i++)
		{
			if (i == 20){ testuje4.ustawError(3);
			dysk.write(i, i);
				testuje4.ustawError(2);
			}
			dysk.write(i, i);
			//System.out.println("Tu wątek 0 odczytałem z sektora: "+i+" wartosc: "+dysk.read(i));
		}
		System.out.println("Skonczylem zapis");


		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				for (int i = 12; i < 34; i++)
				{
					//dysk.write(i,1);
					System.out.println("Tu wątek 2 odczytałem z sektora: "+i+" wartosc: "+dysk.read(i));
				}
			}
		}).start();


		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				for (int i = 24; i< 29; i++)
				{
					//dysk.write(i, 1);
					System.out.println("Tu wątek 3 odczytałem z sektora: "+i+" wartosc: "+dysk.read(i));
				}
			}
		}).start();

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				for (int i = 36; i < 38; i++)
				{
					//dysk.write(i,1);
					System.out.println("Tu wątek 4 odczytałem z sektora: "+i+" wartosc: "+dysk.read(i));
				}
			}
		}).start();


		for (int i = 0; i <21; i++)
		{
			System.out.println("Tu wątek main odczytałem z sektora: "+i+" wartosc: "+dysk.read(i));
		}
		dysk.shutdown();

		System.out.print("\n");
		for (int i = 0; i < 50; i++)
		{
			System.out.println("Tu wątek 1 odczytałem z sektora: "+i+" wartosc: "+dysk.read(i));
		}

	}
}

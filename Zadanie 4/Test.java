import java.rmi.Remote;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class Test {
	public static void main(String[] args) throws Exception {
		Remote r = java.rmi.Naming.lookup("//127.0.0.1:1100/WARD");
		AbstractWard ward = (AbstractWard) r;
		Set<Patient> patients;

		ward.setLimits(new HashMap<Integer, Integer>() {
			{
				put(11, 3);
				put(12, 4);
			}
		});

		Set<Patient> o = ward.getPatients(2);

		if (o == null)
			System.out.println("pusto");

		o = ward.getPatients();

		if (o.isEmpty())
			System.out.println("pusto 2");

		Patient p1 = new Patient();
		Patient p2 = new Patient();
		Patient p3 = new Patient();
		Patient p4 = new Patient();
		Patient p5 = new Patient();
		Patient p6 = new Patient();

		Thread[] threads = new Thread[3];

        threads[0] = new Thread( new Runnable() {
			
			@Override
			public void run() {
				try
				{
					System.out.println("Dodaje pacjenta p1 do pokoju 11: " + ward.patientAdmission(p1, 11));
					System.out.flush();
					System.out.println("Dodaje pacjenta p2 do pokoju 11: " + ward.patientAdmission(p2, 11));
					System.out.flush();
				}
				catch (Exception e)
				{

				}
				
			}
		} );

		threads[1] = new Thread( new Runnable() {
			
			@Override
			public void run() {
				try
				{
					System.out.println("Usuwam pacjenta p1 z pokoju 11: " + ward.patientDischarge(p1));
					System.out.flush();
					System.out.println("Dodaje pacjenta p4 do pokoju 11: " + ward.patientAdmission(p4, 11));
					System.out.flush();
					System.out.println("Usuwam pacjenta p1 z pokoju 11: " + ward.patientDischarge(p1));
					System.out.flush();
				}
				catch (Exception e)
				{

				}
				
			}
		} );

		threads[2] = new Thread( new Runnable() {
			
			@Override
			public void run() {
				try
				{
					System.out.println("Dodaje pacjenta p5 do pokoju 11: " + ward.patientAdmission(p5, 11));
					System.out.flush();
					System.out.println("Dodaje pacjenta p6 do pokoju 12: " + ward.patientAdmission(p6, 12));
					System.out.flush();
				}
				catch (Exception e)
				{

				}
				
			}
		} );

		for (Thread tr : threads)
		{
			tr.start();
		}

		for (Thread tr : threads)
		{
			tr.join();
		}		

		patients = ward.getPatients();

		for(Patient patient : patients) {
            System.out.println(patient.getID());
        }

        for(Map.Entry<Integer, Integer> entry : ward.getRoomsState().entrySet()) {
            System.out.println("Pokoj: " + entry.getKey() + " Ilosc: " + entry.getValue());
        }
	}
}
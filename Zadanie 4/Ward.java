import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

class Ward extends UnicastRemoteObject implements AbstractWard {

	private Map<Integer, Integer> rooms;
    private List<Patient> admissionHistory;
    private Map<Patient, Integer> patients;

    protected Ward() throws RemoteException
    {
    	super();
    }

	public void setLimits(Map<Integer, Integer> max) throws RemoteException
	{
		rooms = new HashMap<Integer, Integer>();
    	admissionHistory = new ArrayList<Patient>();
    	patients = new HashMap<Patient, Integer>();

		if (max != null)
		{
			rooms = max;
		}
	}

	public boolean patientAdmission(Patient patient, int room) throws RemoteException
	{
		synchronized(patients)
		{
			if (rooms.containsKey(room))
			{
				long patientID = patient.getID();

				Iterator<Map.Entry<Patient, Integer>> entries = patients.entrySet().iterator();

				int count = 0;

				while (entries.hasNext())
				{
					Map.Entry<Patient, Integer> entry = entries.next();
					
					if (entry.getKey().getID() == patientID)
					{
						return false;
					}
					else
					{
						if (entry.getValue() == room)
						{
							count++;
						}
					}
				}

				int limit = rooms.get(room);

				if (count < limit)
				{
					patients.put(patient,room);
					admissionHistory.add(patient);
					return true;
				}
				else
				{
					return false;
				}
			}
			else
			{
				return false;
			}
		}
	}

	public boolean patientDischarge(Patient patient) throws RemoteException
	{
		synchronized(patients)
		{
			long patientID = patient.getID();

			Iterator<Map.Entry<Patient, Integer>> entries = patients.entrySet().iterator();

			while (entries.hasNext())
			{
				Map.Entry<Patient, Integer> entry = entries.next();
				
				if (entry.getKey().getID() == patientID)
				{
					entries.remove();
					return true;
				}
			}

			return false;
		}
		
	}

	public Set<Patient> getPatients() throws RemoteException
	{
		Set<Patient> patientsSet = new HashSet<Patient>(patients.keySet());
		return patientsSet;
	}

	public Set<Patient> getPatients(int room) throws RemoteException
	{
		if (rooms.containsKey(room))
		{
			Set<Patient> patientsInRoom = new HashSet<Patient>();

			Iterator<Map.Entry<Patient, Integer>> entries = patients.entrySet().iterator();

			while (entries.hasNext())
			{
				Map.Entry<Patient, Integer> entry = entries.next();
				
				if (entry.getValue() == room)
				{
					patientsInRoom.add(entry.getKey());
				}
			}

            return patientsInRoom;
		}
		else
		{
			return null;
		}
	}

	public Map<Integer, Integer> getRoomsState() throws RemoteException
	{
		Map<Integer, Integer> roomsState = new HashMap<Integer, Integer>();

		for (int room: rooms.keySet()) 
		{
			Iterator<Map.Entry<Patient, Integer>> entries = patients.entrySet().iterator();

			int patientsInRoom = 0;

			while (entries.hasNext())
			{
				Map.Entry<Patient, Integer> entry = entries.next();
				
				if (entry.getValue() == room)
				{
					patientsInRoom++;
				}
			}

			roomsState.put(room, patientsInRoom);
		}

		return roomsState;
	}

	public List<Patient> getAdmissionHistory() throws RemoteException
	{
		return admissionHistory;
	}
}

class Start {
  public static void main(String[] argv) {
  	try
  	{
  		Registry registry = LocateRegistry.getRegistry("localhost", 1099);
		registry.rebind("WARD", new Ward());
  	}
  	catch(Exception e)
	{
	    System.out.println(e);
	}	
  }  
}
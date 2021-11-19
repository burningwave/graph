package org.burningwave.graph.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.burningwave.core.ManagedLogger;
import org.burningwave.graph.Context;
import org.burningwave.graph.bean.Person;

public class ServiceTwo implements ManagedLogger {

	public static final int PERSONS_COLLECTION_SIZE = 10000;
	public static final int PERSONS_COLLECTIONS_NUMBER = 50;

	public void loadCollectionOfCollection(Context data) {
		List<List<Person>> listOfPersonList = new ArrayList<>();
		for (int i = 0; i < PERSONS_COLLECTIONS_NUMBER; i++) {
			listOfPersonList.add(loadCollection());
		}
		data.put("listOfPersonList", listOfPersonList);
	}

	private List<Person> loadCollection() {
		List<Person> list = new ArrayList<>();
		int inputCollectionSize = PERSONS_COLLECTION_SIZE;
		for (int i = 0; i < inputCollectionSize; i++) {
			list.add(new Person());
		}
		return list;
	}

	public void loadCollection(Context data) {
		data.put("persons", loadCollection());
	}

	public void setIdOnPerson(Context data) {
		logInfo("current Index " + data.getCurrentIterationIndex());
		Person person = data.getCurrentIteratedObject();
		person.setId(data.getCurrentIterationIndex());
	}

	public void setNameOnPerson(Context data) {
		logInfo("current Index " + data.getCurrentIterationIndex());
		Person person = data.getCurrentIteratedObject();
		person.setName(getSaltString());
	}

	public void setSurnameOnPerson(Context data) {
		Person person = data.getCurrentIteratedObject();
		person.setSurname(getSaltString());
	}

	public void setAddressOnPerson(Context data) {
		Person person = data.getCurrentIteratedObject();
		person.setAddress(getSaltString());
		data.setCurrentIterationResult(person);
	}

	public void printAllPersons(Context data) {
		List<Person> persons = data.get("persons");
		persons.forEach(person ->
			logInfo("name:{}, surname: {}, address:{}", person.getName(), person.getSurname(), person.getAddress())
		);
	}

	public void printAllArrayOfArrayOfPersons(Context data) {
		AtomicInteger index = new AtomicInteger(0);
		Object[] arryayOfArrayOfObjects = data.get("outputArray");
		Stream.of(arryayOfArrayOfObjects).forEach(arrayOfObjects ->
			Stream.of((Object[])arrayOfObjects).forEach(object -> {
				Person person = (Person)object;
				logInfo("person id: {}, name: {}, surname: {}, address: {}, number: {}", person.getId(), person.getName(), person.getSurname(), person.getAddress(), index.incrementAndGet());
			})
		);
		data.put("total", index.get());
	}

	protected String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 18) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

}

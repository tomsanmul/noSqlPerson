package dev.cifo.noSqlPerson;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.ArrayList;
import java.util.List;

class NoSqlPersonApplicationTests {

	static PersonService personService;

	@BeforeAll
	static void setup() {
		DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
		DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
				.dynamoDbClient(dynamoDbClient)
				.build();
		PersonEventPublisher eventPublisher = new PersonEventPublisher();
		personService = new PersonService(enhancedClient, eventPublisher);
	}


	@Test
	void createPerson() {
		String id = "WEB_APP_2029";
		List<Person> persons = new ArrayList<>();

		// 500 students
		long genStart = System.currentTimeMillis();
		for (int i = 1; i <= 500; i++) {
			Person p = new Person();
			p.setCourseId(id);
			p.setCourseItem(String.format("STUDENT#%04d", i));
			p.setName("Student " + i);
			p.setAge(18 + (i % 10));
			p.setEmail("student" + i + "@school.dev");
			p.setSchoolId("SCHOOL_BCN");
			p.setSchoolItem("STUDENT#" + i);
			persons.add(p);
		}

		// 40 teachers
		for (int i = 1; i <= 40; i++) {
			Person p = new Person();
			p.setCourseId(id);
			p.setCourseItem(String.format("TEACHER#%04d", i));
			p.setName("Teacher " + i);
			p.setAge(30 + (i % 20));
			p.setEmail("teacher" + i + "@school.dev");
			p.setSchoolId("SCHOOL_BCN");
			p.setSchoolItem("TEACHER#" + i);
			persons.add(p);
		}

		// 50 staff
		for (int i = 1; i <= 50; i++) {
			Person p = new Person();
			p.setCourseId(id);
			p.setCourseItem(String.format("STAFF#%04d", i));
			p.setName("Staff " + i);
			p.setAge(25 + (i % 15));
			p.setEmail("staff" + i + "@school.dev");
			p.setSchoolId("SCHOOL_BCN");
			p.setSchoolItem("STAFF#" + i);
			persons.add(p);
		}

		// 50 legal
		for (int i = 1; i <= 50; i++) {
			Person p = new Person();
			p.setCourseId(id);
			p.setCourseItem(String.format("LEGAL#%04d", i));
			p.setName("Legal " + i);
			p.setAge(28 + (i % 12));
			p.setEmail("legal" + i + "@school.dev");
			p.setSchoolId("SCHOOL_BCN");
			p.setSchoolItem("LEGAL#" + i);
			persons.add(p);
		}
		long genEnd = System.currentTimeMillis();

		System.out.println("========== FAKE DATA GENERATION ==========");
		System.out.println("Total persons generated: " + persons.size());
		System.out.println("Generation time: " + (genEnd - genStart) + " ms");

		// Batch write with latency metrics
		long writeStart = System.currentTimeMillis();
		int batchCount = personService.saveBatch(persons);
		long writeEnd = System.currentTimeMillis();

		long totalWriteMs = writeEnd - writeStart;
		double avgPerItem = (double) totalWriteMs / persons.size();
		double avgPerBatch = (double) totalWriteMs / batchCount;

		System.out.println("========== BATCH WRITE METRICS ==========");
		System.out.println("Total items written: " + persons.size());
		System.out.println("  - Students: 500");
		System.out.println("  - Teachers: 40");
		System.out.println("  - Staff:    50");
		System.out.println("  - Legal:    50");
		System.out.println("Batches (25 items each): " + batchCount);
		System.out.println("Total write time:       " + totalWriteMs + " ms");
		System.out.println("Avg latency per item:   " + String.format("%.2f", avgPerItem) + " ms");
		System.out.println("Avg latency per batch:  " + String.format("%.2f", avgPerBatch) + " ms");
		System.out.println("Throughput:             " + String.format("%.0f", persons.size() / (totalWriteMs / 1000.0)) + " items/sec");
		System.out.println("==========================================");

		// Print sample data from each category
		System.out.println("\n--- Sample Data (2 per category) ---");
		persons.stream().filter(p -> p.getCourseItem().startsWith("STUDENT")).limit(2).forEach(System.out::println);
		persons.stream().filter(p -> p.getCourseItem().startsWith("TEACHER")).limit(2).forEach(System.out::println);
		persons.stream().filter(p -> p.getCourseItem().startsWith("STAFF")).limit(2).forEach(System.out::println);
		persons.stream().filter(p -> p.getCourseItem().startsWith("LEGAL")).limit(2).forEach(System.out::println);
	}

	@Test
	void getAllPersonsByCourseId() {
		String id = "WEB_APP_2027";

		// Query all items for this partition key
		long queryStart = System.currentTimeMillis();
		List<Person> persons = new ArrayList<>();
		personService.queryById(id).forEach(page -> persons.addAll(page.items()));
		long queryEnd = System.currentTimeMillis();

		long totalQueryMs = queryEnd - queryStart;

		// Count by category
		long students = persons.stream().filter(p -> p.getCourseItem() != null && p.getCourseItem().startsWith("STUDENT")).count();
		long teachers = persons.stream().filter(p -> p.getCourseItem() != null && p.getCourseItem().startsWith("TEACHER")).count();
		long staff = persons.stream().filter(p -> p.getCourseItem() != null && p.getCourseItem().startsWith("STAFF")).count();
		long legal = persons.stream().filter(p -> p.getCourseItem() != null && p.getCourseItem().startsWith("LEGAL")).count();

		System.out.println("========== QUERY BY COURSE ID ==========");
		System.out.println("CourseId: " + id);
		System.out.println("Total items retrieved: " + persons.size());
		System.out.println("  - Students: " + students);
		System.out.println("  - Teachers: " + teachers);
		System.out.println("  - Staff:    " + staff);
		System.out.println("  - Legal:    " + legal);
		System.out.println("Total query time:       " + totalQueryMs + " ms");
		System.out.println("Avg latency per item:   " + String.format("%.2f", (double) totalQueryMs / persons.size()) + " ms");
		System.out.println("Throughput:             " + String.format("%.0f", persons.size() / (totalQueryMs / 1000.0)) + " items/sec");
		System.out.println("==========================================");

		// Print sample data from each category
		System.out.println("\n--- Sample Data (2 per category) ---");
		persons.stream().filter(p -> p.getCourseItem().startsWith("STUDENT")).limit(2).forEach(System.out::println);
		persons.stream().filter(p -> p.getCourseItem().startsWith("TEACHER")).limit(2).forEach(System.out::println);
		persons.stream().filter(p -> p.getCourseItem().startsWith("STAFF")).limit(2).forEach(System.out::println);
		persons.stream().filter(p -> p.getCourseItem().startsWith("LEGAL")).limit(2).forEach(System.out::println);
	}
}

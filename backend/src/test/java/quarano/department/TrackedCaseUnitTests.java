package quarano.department;

import static java.time.LocalDate.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.Mockito.*;

import quarano.account.Account;
import quarano.account.Department;
import quarano.core.EmailAddress;
import quarano.core.PhoneNumber;
import quarano.department.TrackedCase.Status;
import quarano.tracking.ContactPerson;
import quarano.tracking.ContactWays;
import quarano.tracking.TrackedPerson;
import quarano.tracking.TrackedPersonDataInitializer;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link TrackedCase}.
 *
 * @author Oliver Drotbohm
 */
class TrackedCaseUnitTests {

	private static TrackedCase prepareTrackedCaseForCompletion(TrackedPerson person) {

		var department = new Department("Musterstadt", UUID.randomUUID());

		return new TrackedCase(person, CaseType.INDEX, department) //
				.submitEnrollmentDetails() //
				.submitQuestionnaire(new MinimalQuestionnaire());
	}

	@Test
	void rejectsEnrollmentCompletionForRequiredEncountersButNoneRegistered() {

		var person = TrackedPersonDataInitializer.createTanja();
		var trackedCase = prepareTrackedCaseForCompletion(person);

		assertThatExceptionOfType(EnrollmentException.class)
				.isThrownBy(() -> trackedCase.markEnrollmentCompleted(EnrollmentCompletion.WITH_ENCOUNTERS));
		assertThat(trackedCase.isEnrollmentCompleted()).isFalse();
	}

	@Test
	void allowsEnrollmentCompletionWithoutEncountersIfExplicitlyAcknowledged() {

		var person = TrackedPersonDataInitializer.createTanja();
		var trackedCase = prepareTrackedCaseForCompletion(person);

		assertThatCode(() -> trackedCase.markEnrollmentCompleted(EnrollmentCompletion.WITHOUT_ENCOUNTERS)) //
				.doesNotThrowAnyException();
		assertThat(trackedCase.isEnrollmentCompleted()).isTrue();
	}

	@Test
	void allowsCompletionWithEncountersIfRegistered() {

		var person = TrackedPersonDataInitializer.createTanja();
		var trackedCase = prepareTrackedCaseForCompletion(person);

		var contact = new ContactPerson("Michaela", "Mustermann", ContactWays.ofEmailAddress("michaela@mustermann.de"));
		person.reportContactWith(contact, now());

		assertThatCode(() -> trackedCase.markEnrollmentCompleted(EnrollmentCompletion.WITH_ENCOUNTERS)) //
				.doesNotThrowAnyException();
		assertThat(trackedCase.isEnrollmentCompleted()).isTrue();
	}

	@Test
	void reopensEnrollment() {

		var person = TrackedPersonDataInitializer.createTanja();
		var trackedCase = prepareTrackedCaseForCompletion(person);

		assertThatCode(() -> trackedCase.markEnrollmentCompleted(EnrollmentCompletion.WITHOUT_ENCOUNTERS)) //
				.doesNotThrowAnyException();
		assertThat(trackedCase.isEnrollmentCompleted()).isTrue();

		trackedCase.reopenEnrollment();

		assertThat(trackedCase.getEnrollment().isComplete()).isFalse();
	}

	@Test
	void rejectDetailsSubmissionIfIncomplete() {

		var person = TrackedPersonDataInitializer.createMarkus();
		var department = new Department("Musterstadt", UUID.randomUUID());

		var trackedCase = new TrackedCase(person, CaseType.INDEX, department);

		assertThatExceptionOfType(EnrollmentException.class) //
				.isThrownBy(() -> trackedCase.submitQuestionnaire(new MinimalQuestionnaire()));
	}

	@Test
	void initializesCaseWithRegistrationCompletedIfPersonHasAccount() {

		var account = mock(Account.class);
		var markus = TrackedPersonDataInitializer.createMarkus().markAccountRegistration(account);
		var department = new Department("Musterstadt", UUID.randomUUID());

		assertThat(new TrackedCase(markus, CaseType.INDEX, department).getStatus()).isEqualTo(Status.REGISTERED);
	}

	@ParameterizedTest
	@MethodSource(value = "initialCallSource")
	void testIsInitialCallNeeded(Status status, TrackedPerson trackedPerson, boolean isNeeded) {

		var trackedCase = new TrackedCase(trackedPerson, CaseType.INDEX, new Department("Musterstadt")).setStatus(status);

		assertThat(trackedCase.isInitialCallNeeded()).isEqualTo(isNeeded);
	}

	@SuppressWarnings("null")
	private static Stream<Arguments> initialCallSource() {

		return Stream.of(//
				arguments(Status.OPEN, new TrackedPerson("firstName", "lastName") //
						.setDateOfBirth(now()) //
						.setEmailAddress(EmailAddress.of("test@test.de")) //
						.setPhoneNumber(PhoneNumber.of("0123901")), true), //
				arguments(Status.OPEN, new TrackedPerson("firstName", "lastName") //
						.setDateOfBirth(now()) //
						.setEmailAddress(EmailAddress.of("test@test.de")) //
						.setMobilePhoneNumber(PhoneNumber.of("0123901")), true), //
				arguments(Status.TRACKING, new TrackedPerson("firstName", "lastName") //
						.setDateOfBirth(now()) //
						.setEmailAddress(EmailAddress.of("test@test.de")) //
						.setPhoneNumber(PhoneNumber.of("0123901")), false),
				arguments(Status.OPEN, new TrackedPerson("firstName", "lastName") //
						.setDateOfBirth(null) //
						.setEmailAddress(EmailAddress.of("test@test.de")) //
						.setPhoneNumber(PhoneNumber.of("0123901")), false), //
				arguments(Status.OPEN, new TrackedPerson("firstName", "lastName") //
						.setDateOfBirth(now()) //
						.setEmailAddress(null) //
						.setPhoneNumber(PhoneNumber.of("0123901")), false), //
				arguments(Status.OPEN, new TrackedPerson("firstName", "lastName") //
						.setDateOfBirth(now()) //
						.setEmailAddress(EmailAddress.of("test@test.de")) //
						.setPhoneNumber(null) //
						.setMobilePhoneNumber(null), false));
	}

	@ParameterizedTest
	@MethodSource(value = "missingDetailsSource")
	void testIsInvestigationNeeded(Status status, TrackedPerson trackedPerson, boolean isNeeded) {
		var trackedCase = new TrackedCase(trackedPerson, CaseType.INDEX, new Department("Musterstadt")).setStatus(status);
		assertThat(trackedCase.isInvestigationNeeded()).isEqualTo(isNeeded);
	}

	private static Stream<Arguments> missingDetailsSource() {

		return Stream.of( //
				arguments(Status.REGISTERED, new TrackedPerson("firstName", "lastName"), true),
				arguments(Status.REGISTERED, new TrackedPerson("firstName", "lastName") //
						.setEmailAddress(EmailAddress.of("test@test.de")) //
						.setDateOfBirth(now()), true),
				arguments(Status.REGISTERED, new TrackedPerson("firstName", "lastName") //
						.setPhoneNumber(PhoneNumber.of("0123901")) //
						.setMobilePhoneNumber(PhoneNumber.of("0123901293")) //
						.setDateOfBirth(now()), true),
				arguments(Status.REGISTERED, new TrackedPerson("firstName", "lastName") //
						.setEmailAddress(EmailAddress.of("test@test.de")) //
						.setPhoneNumber(PhoneNumber.of("0123901")) //
						.setMobilePhoneNumber(PhoneNumber.of("0123901293")), true),
				arguments(Status.REGISTERED, new TrackedPerson("firstName", "lastName") //
						.setEmailAddress(EmailAddress.of("test@test.de")) //
						.setPhoneNumber(PhoneNumber.of("0123901")) //
						.setMobilePhoneNumber(PhoneNumber.of("0123901293")).setDateOfBirth(now()), false),
				arguments(Status.CONCLUDED, new TrackedPerson("firstName", "lastName"), false));
	}
}

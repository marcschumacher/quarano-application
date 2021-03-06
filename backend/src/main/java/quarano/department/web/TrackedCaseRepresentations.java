package quarano.department.web;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import quarano.account.Account;
import quarano.account.Department;
import quarano.core.PhoneNumber;
import quarano.core.validation.Email;
import quarano.core.validation.Strings;
import quarano.core.validation.Textual;
import quarano.core.web.ErrorsDto;
import quarano.core.web.MapperWrapper;
import quarano.department.CaseType;
import quarano.department.Comment;
import quarano.department.Questionnaire;
import quarano.department.Questionnaire.SymptomInformation;
import quarano.department.TrackedCase;
import quarano.reference.SymptomRepository;
import quarano.tracking.TrackedPerson;
import quarano.tracking.ZipCode;
import quarano.tracking.web.TrackedPersonDto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.groups.Default;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * @author Oliver Drotbohm
 */
@Component
@RequiredArgsConstructor
class TrackedCaseRepresentations implements ExternalTrackedCaseRepresentations {

	private final MapperWrapper mapper;
	private final SmartValidator validator;
	private final MessageSourceAccessor messages;
	private final @NonNull SymptomRepository symptoms;

	TrackedCaseDto toInputRepresentation(TrackedCase trackedCase) {

		var personDto = mapper.map(trackedCase.getTrackedPerson(), TrackedPersonDto.class);
		var caseDto = mapper.map(trackedCase, TrackedCaseDto.class);

		return mapper.map(personDto, caseDto);
	}

	TrackedCaseDetails toRepresentation(TrackedCase trackedCase) {

		var dto = toInputRepresentation(trackedCase);

		return new TrackedCaseDetails(trackedCase, dto, messages);
	}

	public TrackedCaseSummary toSummary(TrackedCase trackedCase) {
		return new TrackedCaseSummary(trackedCase, messages);
	}

	QuestionnaireDto toRepresentation(Questionnaire report) {
		return mapper.map(report, QuestionnaireDto.class);
	}

	TrackedCase from(TrackedCaseDto source, TrackedCase existing, ErrorsDto errors) {

		validateForUpdate(source, existing, errors);

		if (errors.hasErrors()) {
			return existing;
		}

		var personSource = mapper.map(source, TrackedPersonDto.class);
		mapper.map(personSource, existing.getTrackedPerson());

		var result = mapper.map(source, existing);

		return mapper.map(source, result).markEdited();
	}

	TrackedCase from(TrackedCaseDto source, Department department, CaseType type, ErrorsDto errors) {

		validateForCreation(source, type, errors);

		var personDto = mapper.map(source, TrackedPersonDto.class);
		var person = mapper.map(personDto, TrackedPerson.class);

		return mapper.map(source, new TrackedCase(person, type, department));
	}

	private Questionnaire createQuestionnaireFrom(QuestionnaireDto source) {

		SymptomInformation symptomsInfo;

		if (source.getHasSymptoms()) {

			var result = source.getSymptoms().stream() //
					.map(it -> symptoms.findById(it).get()) //
					.collect(Collectors.toList());

			symptomsInfo = SymptomInformation.withSymptomsSince(source.getDayOfFirstSymptoms(), result);

		} else {
			symptomsInfo = SymptomInformation.withoutSymptoms();
		}

		return new Questionnaire(symptomsInfo, source.getHasPreExistingConditionsDescription(),
				source.getBelongToMedicalStaffDescription());
	}

	Questionnaire from(QuestionnaireDto source) {

		var report = createQuestionnaireFrom(source);
		return source.applyTo(mapper.map(source, report), symptoms);

	}

	Questionnaire from(QuestionnaireDto source, Questionnaire existing) {
		return source.applyTo(mapper.map(source, existing), symptoms);
	}

	Comment from(CommentInput payload, Account account) {
		return new Comment(payload.getComment(), account.getFullName());
	}

	private ErrorsDto validateForUpdate(TrackedCaseDto payload, TrackedCase existing, ErrorsDto errors) {

		if (existing.isEnrollmentCompleted()) {
			errors = validateAfterEnrollment(payload, errors);
		}

		return validate(payload, existing.getType(), errors);
	}

	private ErrorsDto validateForCreation(TrackedCaseDto payload, CaseType type, ErrorsDto errors) {

		if (type.equals(CaseType.CONTACT)) {
			errors.rejectField(payload.isInfected(), "infected", "ContactCase.infected");
			errors.rejectField(payload.getTestDate() != null, "testDate", "ContactCase.infected");
		}

		return validate(payload, type, errors);
	}

	private ErrorsDto validate(TrackedCaseDto payload, CaseType type, ErrorsDto errors) {

		if (payload.getTestDate() == null && payload.isInfected()) {
			errors.rejectField("testDate", "ContactCase.infected");
		}

		var validationGroups = new ArrayList<>();
		validationGroups.add(Default.class);

		if (type.equals(CaseType.INDEX) || payload.getTestDate() != null) {
			validationGroups.add(ValidationGroups.Index.class);
		}

		return errors //
				.doWith(it -> validator.validate(payload, it, validationGroups.toArray())) //
				.doWith(it -> payload.validate(it, type));
	}

	private ErrorsDto validateAfterEnrollment(TrackedCaseDto source, ErrorsDto errors) {

		TrackedPersonDto dto = mapper.map(source, TrackedPersonDto.class);

		return errors.doWith(it -> validator.validate(dto, it));
	}

	@Data
	@Getter(onMethod = @__(@Nullable))
	@NoArgsConstructor
	static class TrackedCaseDto {

		private @Pattern(regexp = Strings.NAMES) @NotEmpty String lastName;
		private @Pattern(regexp = Strings.NAMES) @NotEmpty String firstName;
		private @NotNull(groups = ValidationGroups.Index.class) LocalDate testDate;
		private @NotNull(groups = ValidationGroups.Index.class) LocalDate quarantineStartDate;
		private @NotNull(groups = ValidationGroups.Index.class) LocalDate quarantineEndDate;

		private @Pattern(regexp = Strings.STREET) String street;
		private @Pattern(regexp = Strings.HOUSE_NUMBER) String houseNumber;
		private @Pattern(regexp = Strings.CITY) String city;
		private @Pattern(regexp = ZipCode.PATTERN) String zipCode;
		private @Pattern(regexp = PhoneNumber.PATTERN) String mobilePhone;
		private @Pattern(regexp = PhoneNumber.PATTERN) String phone;
		private @Email String email;
		private @Past LocalDate dateOfBirth;
		private @Getter boolean infected;

		Errors validate(Errors errors, CaseType type) {

			if (type.equals(CaseType.CONTACT)) {
				return errors;
			}

			if (!StringUtils.hasText(phone) && !StringUtils.hasText(mobilePhone)) {
				errors.rejectValue("phone", "PhoneOrMobile");
				errors.rejectValue("mobilePhone", "PhoneOrMobile");
			}

			return errors;
		}
	}

	@Relation(collectionRelation = "cases")
	static class TrackedCaseDetails extends TrackedCaseStatusAware<TrackedCaseDetails> {

		private final TrackedCase trackedCase;
		private final TrackedCaseSummary summary;
		private final @Getter(onMethod = @__(@JsonUnwrapped)) TrackedCaseDto dto;

		public TrackedCaseDetails(TrackedCase trackedCase, TrackedCaseDto dto, MessageSourceAccessor messages) {

			super(trackedCase, messages);

			this.trackedCase = trackedCase;
			this.dto = dto;
			this.summary = new TrackedCaseSummary(trackedCase, messages);
		}

		public String getCaseId() {
			return trackedCase.getId().toString();
		}

		public String getCaseTypeLabel() {
			return summary.getCaseTypeLabel();
		}

		public List<CommentRepresentation> getComments() {

			return getTrackedCase().getComments().stream() //
					.sorted(Comparator.comparing(Comment::getDate).reversed()) //
					.map(CommentRepresentation::new) //
					.collect(Collectors.toUnmodifiableList());
		}
	}

	@RequiredArgsConstructor
	static class CommentRepresentation {

		private final Comment comment;

		public LocalDateTime getDate() {
			return comment.getDate();
		}

		public String getComment() {
			return comment.getText();
		}

		public String getAuthor() {
			return comment.getAuthor();
		}
	}

	@Data
	static class CommentInput {
		@Textual String comment;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@Validated({ Default.class, ValidationGroups.Index.class })
	static @interface ValidatedIndexCase {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.PARAMETER)
	@Validated
	static @interface ValidatedContactCase {
	}

	static class ValidationGroups {

		interface Index {}

		interface Contact {}
	}
}

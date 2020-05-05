package quarano.account;

import io.vavr.control.Try;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import quarano.account.Account.AccountIdentifier;
import quarano.account.Department.DepartmentIdentifier;
import quarano.account.Password.EncryptedPassword;
import quarano.account.Password.UnencryptedPassword;

import java.util.List;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Transactional
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountService {

	private final @NonNull PasswordEncoder passwordEncoder;
	private final @NonNull AccountRepository accounts;
	private final @NonNull RoleRepository roles;

	/**
	 * creates a new account, encrypts the password and stores it
	 *
	 * @param username
	 * @param password
	 * @param firstname
	 * @param lastname
	 * @param departmentId
	 * @param clientId
	 * @param roleType
	 * @return
	 */
	public Account createTrackedPersonAccount(String username, UnencryptedPassword password, String firstname,
			String lastname, DepartmentIdentifier departmentId) {

		var encryptedPassword = EncryptedPassword.of(passwordEncoder.encode(password.asString()));
		var role = roles.findByName(RoleType.ROLE_USER.toString());
		var account = accounts.save(new Account(username, encryptedPassword, firstname, lastname, departmentId, role));

		return account;
	}

	public Account createStaffAccount(String username, UnencryptedPassword password, String firstname, String lastname,
			DepartmentIdentifier departmentId, RoleType roleType) {

		var encryptedPassword = EncryptedPassword.of(passwordEncoder.encode(password.asString()));
		var role = roles.findByName(roleType.toString());
		var account = accounts.save(new Account(username, encryptedPassword, firstname, lastname, departmentId, role));

		log.info("Created staff account for " + username);

		return account;
	}

	public boolean isValidUsername(String candidate) {

		if (!isUsernameAvailable(candidate)) {
			return false;
		}
		// check username pattern
		return true;
	}

	public Optional<Account> findByUsername(String username) {
		return accounts.findByUsername(username);
	}

	public boolean isUsernameAvailable(String userName) {
		return accounts.findByUsername(userName).isEmpty();
	}

	public boolean matches(@Nullable UnencryptedPassword candidate, EncryptedPassword existing) {

		Assert.notNull(existing, "Existing password must not be null!");

		return Optional.ofNullable(candidate).//
				map(c -> passwordEncoder.matches(c.asString(), existing.asString())).//
				orElse(false);
	}

	public List<Account> findStaffAccountsFor(DepartmentIdentifier departmentId) {
		return accounts.findStaffAccountsFor(departmentId);
	}

	public Optional<Account> findById(AccountIdentifier accountId) {
		return accounts.findById(accountId);
	}

	public Try<Account> deleteAccount(AccountIdentifier accountIdToDelete, Account deletingAdminAccount) {

		// check if deleting user belongs to same department and is admin
		Try<Account> accountTry = Try.ofSupplier(() -> accounts.findById(accountIdToDelete).get())
				.andThenTry(it -> accountBelongsToDepartmentOfAdmin(it, deletingAdminAccount))
				.andThen(it -> deletingAdminAccount.hasAdminRole());

		return accountTry.onSuccess(it -> accounts.deleteById(accountIdToDelete));
	}

	public Account addStaffAccount(Object object) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean accountBelongsToDepartmentOfAdmin(Account accountToDelete, Account accountOfDeletingUser)
			throws InvalidAdminAccessException {

		if (accountToDelete.belongsTo(accountOfDeletingUser.getDepartmentId())) {
			return true;
		} else {
			throw new InvalidAdminAccessException(
					"Admin-user does not belong to the same department as the acount that should be deleted; admin-user department: "
							+ accountOfDeletingUser.getDepartmentId() + ", target account department: "
							+ accountToDelete.getDepartmentId());
		}
	}
}

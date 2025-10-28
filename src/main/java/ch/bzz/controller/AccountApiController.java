package ch.bzz.controller;

import ch.bzz.generated.api.AccountApi;
import ch.bzz.generated.model.Account;
import ch.bzz.generated.model.UpdateAccountsRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class AccountApiController implements AccountApi {
    @Override
    public ResponseEntity<List<Account>> getAccounts() {
        return null;
    }

    @Override
    public ResponseEntity<Void> updateAccounts(UpdateAccountsRequest updateAccountsRequest) {
        return null;
    }
}

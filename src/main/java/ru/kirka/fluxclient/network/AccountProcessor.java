package ru.kirka.fluxclient.network;

import java.util.ArrayList;
import java.util.List;

public class AccountProcessor {
   private final List<AccountConstructor> accounts = new ArrayList<>();

   public AccountProcessor() {
      this.accounts.add(new AccountConstructor("Kirka_int", true, false));
   }

   public List<AccountConstructor> getAccounts() {
      return this.accounts;
   }

   public List<AccountConstructor> a() {
      return this.accounts;
   }
}

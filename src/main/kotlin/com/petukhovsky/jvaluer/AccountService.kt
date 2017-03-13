package com.petukhovsky.jvaluer

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


/**
 * Created by arthur on 21.02.17.
 */
@Service
class AccountService
@Autowired
constructor(
        val accountRepository: AccountRepository
) : UserDetailsService {

    val passwordEncoder = com.petukhovsky.jvaluer.passwordEncoder()

    fun save(account: Account): Account {
        account.password = passwordEncoder.encode(account.password)
        return accountRepository.save(account)
    }

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String?): UserDetails {
        println("Username: " + username)
        val account =
                accountRepository.findOneByUsername(username)
                        ?: throw UsernameNotFoundException("user not found")
        return createUser(account)
    }

    internal fun signIn(account: Account) {
        SecurityContextHolder.getContext().authentication = authenticate(account)
    }

    private fun authenticate(account: Account): Authentication {
        return UsernamePasswordAuthenticationToken(createUser(account), account, account.grantedAuthorities)
    }

    private fun createUser(account: Account): User {
        return User(account.username, account.password, account.grantedAuthorities)
    }
}

private val Account.grantedAuthorities: MutableCollection<out GrantedAuthority>?
    get() {
        return mutableListOf(SimpleGrantedAuthority(this.role))
    }

@Document
data class Account(
        @Id var id: String? = null,
        @Indexed(unique = true) var username: String = "",
        @JsonIgnore var password: String = "",
        var role: String = "ROLE_USER"
)

open interface AccountRepository : MongoRepository<Account, String> {
    fun findOneByUsername(username: String?) : Account?
}

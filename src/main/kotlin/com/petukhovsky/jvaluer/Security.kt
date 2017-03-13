package com.petukhovsky.jvaluer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices
import org.springframework.security.crypto.password.StandardPasswordEncoder


/**
 * Created by arthur on 21.02.17.
 */

@EnableWebSecurity
open class SecurityConfig @Autowired constructor(
        val accountService: AccountService
) : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http
                .csrf().disable()
                .authorizeRequests()
                    .antMatchers("/**", "/index").permitAll()
                    .and()
                .formLogin()
                    .loginPage("/")
                    .permitAll()
                    .loginProcessingUrl("/auth")
                    .usernameParameter("user")
                    .passwordParameter("pass")
                    .and()
                .logout()
                    .logoutUrl("/logout")
                    .permitAll()
//                .antMatchers("/user/**").hasRole("USER")
//                .and()
//                .formLogin()
//                .loginPage("/login").failureUrl("/login-error")

    }

    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        auth
            .eraseCredentials(true)
            .userDetailsService(accountService)
            .passwordEncoder(passwordEncoder())
    }
}
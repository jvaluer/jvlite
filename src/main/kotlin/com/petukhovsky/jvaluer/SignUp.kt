package com.petukhovsky.jvaluer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam

/**
 * Created by arthur on 21.02.17.
 */
@Controller
open class SignUpController @Autowired constructor(
    val accountService: AccountService
) {
    @RequestMapping(value = "signup", method = arrayOf(RequestMethod.POST))
    fun signup(
            @RequestParam("user") username: String,
            @RequestParam("pass") password: String
    ) : String {
        var account = Account(
                username = username,
                password = password
        )
        try {
            account = accountService.save(account)
        } catch (e: Exception) {
            return "redirect:/"
        }
        accountService.signIn(account)
        return "redirect:/"
    }
}
---
layout: docs
title: Syntax DSL
permalink: /docs/optics/syntax/
---

## Syntax DSL

In some cases the full power of Optics is not required and a familiar property-like syntax to work with values of immutable structures is desired.
To avoid boilerplate Arrow can generate this property-like dsl using `@optics` annotation.

```kotlin
package com.example.domain

@optics data class Street(val number: Int, val name: String)
@optics data class Address(val city: String, val street: Street)
@optics data class Company(val name: String, val address: Address)
@optics data class Employee(val name: String, val company: Company?)
```

The DSL will be generated in a syntax sub-package of your `data class` and can be used by invoking `setter()` on an instance. i.e. for a package `com.example.domain` the DSL will be generated in `com.example.domain.syntax`.

```kotlin:ank
import com.example.domain.*
import com.example.domain.syntax.*

val employee = Employee("John Doe", Company("Kategory", Address("Functional city", Street(42, "lambda street"))))

employee.setter().company.address.street.name.modify(String::toUpperCase)
```

Arrow can also generate a dsl for a `sealed class` which can be helpful to reduce boilerplate code, or improve readability.

```kotlin:ank:silent
@optics sealed class NetworkResult
@optics data class Success(val content: String): NetworkResult()
@optics sealed class NetworkError : NetworkResult()
@optics data class HttpError(val message: String): NetworkResult()
@optics object TimeoutError: NetworkError()

val networkResult: NetworkResult = HttpError("BOOM!")
```

Let's imagine we have a function `f` of type `(HttpError) -> HttpError` and we want to invoke it on the `NetworkResult`.

```
val f: (String) -> String = String::toUpperCase

when (networkResult) {
  is HttpError -> networkResult.copy(f(networkResult.message))
  else -> networkResult
}
```

We can rewrite this code with our generated dsl.

```kotlin:ank
networkResult.setter().networkError.httpError.message.modify(f)
```

## Syntax DSL vs Optics

In contrast to regular optics the DSL is bound to a value. Lets also generate `Lens` and `Optional` for our above domain so we can compare the difference.
In above example we want to apply a function `f` to the `Street::name` of the `Company`'s `Address` of an `Employee`.

We can compose an `Optic` that achieves the same goal.

```kotlin:ank
import arrow.optics.Optional

val employeesStreetName: Optional<Employee, String> = employeeCompany() compose companyAddress() compose addressStreet() compose streetName()
```

The result is an `Optional` since `Employee::company` is nullable. The `Optional` can be used to apply a function `f` to any `Employee`.

```kotlin:ank
employeesStreetName.modify(employee, String::toUpperCase)
```

So while Optics are more powerful, the syntax DSL can help you write elegant and concise code.

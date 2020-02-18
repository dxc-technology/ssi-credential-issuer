package com.luxoft.ssi.web.data

data class Response(val body: String)

data class Invite(val invite: String)

data class IssueCredentialsRequest(val listOfCredentials: Map<Int, Map<String, String>>)

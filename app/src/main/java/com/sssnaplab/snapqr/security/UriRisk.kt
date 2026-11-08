package com.sssnaplab.snapqr.security

import android.net.Uri

fun UrlRiskAnalyzer.analyze(uri: Uri): RiskReport =
    analyze(uri.scheme, uri.host, uri.userInfo, uri.port, uri.path)

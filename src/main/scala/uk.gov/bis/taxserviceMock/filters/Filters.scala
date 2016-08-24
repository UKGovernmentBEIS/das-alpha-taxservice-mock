package uk.gov.bis.taxserviceMock.filters

import javax.inject.Inject

import play.api.http.DefaultHttpFilters

class Filters @Inject()(log: LoggingFilter) extends DefaultHttpFilters(log)
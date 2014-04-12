package com.flipcast.push.gcm.model

/**
 * Model class that represents GCM result
 *
 * @author Phaneesh Nagaraja
 */
case class GcmResponse(multicast_id: Option[String],
                       success: Int = 0, failure: Int = 0, canonical_ids: Int = 0,
                       results: List[GcmResult])
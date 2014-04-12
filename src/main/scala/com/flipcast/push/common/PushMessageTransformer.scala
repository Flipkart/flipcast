package com.flipcast.push.common

import com.flipcast.push.model.PushMessageData

/**
 * Trait to define message transformations
 *
 * @author Phaneesh Nagaraja
 */
trait PushMessageTransformer {

  def transform(configName: String, message: String) : PushMessageData

}

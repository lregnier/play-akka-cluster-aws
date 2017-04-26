package conf

import java.io.{ BufferedReader, InputStreamReader }
import java.net.URL

import com.amazonaws.auth.InstanceProfileCredentialsProvider
import com.amazonaws.regions.{ Region, Regions }
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.{ DescribeAutoScalingGroupsRequest, DescribeAutoScalingInstancesRequest }
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.{ DescribeInstancesRequest, Instance, InstanceStateName }

import scala.collection.JavaConversions._
import scala.util.{ Success, Try }

class EC2ClientAPI(scaling: AmazonAutoScalingClient, ec2: AmazonEC2Client) {
  import EC2ClientAPI._

  def getSiblingInstancesIps(): Try[List[String]] = {
    def getGroupName(instanceId: String): Try[String] = Try {
      val result = scaling.describeAutoScalingInstances(new DescribeAutoScalingInstancesRequest {
        setInstanceIds(Seq(instanceId))
      })
      result.getAutoScalingInstances.head.getAutoScalingGroupName
    }

    def getGroupInstanceIds(groupName: String): Try[List[String]] = Try {
      val result = scaling.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest {
        setAutoScalingGroupNames(Seq(groupName))
      })
      result.getAutoScalingGroups.head.getInstances.toList map (_.getInstanceId)
    }

    def isRunning(instance: Instance): Boolean = {
      instance.getState.getName == InstanceStateName.Running.toString
    }

    val result: Try[List[String]] =
      for {
        instanceId <- getInstanceId()
        groupName <- getGroupName(instanceId)
        instanceIds <- getGroupInstanceIds(groupName)
      } yield {
        instanceIds map getInstance collect {
          case Success(instance) if isRunning(instance) => instance.getPrivateIpAddress
        }
      }

    result.recover {
      case e: Throwable => throw EC2ClientAPIException("Error while retrieving sibling instances ip", e)
    }
  }

  def getCurrentInstanceIp(): Try[String] = {
    val result: Try[String] =
      for {
        instanceId <- getInstanceId()
        instance <- getInstance(instanceId)
      } yield instance.getPrivateIpAddress

    result.recover {
      case e: Throwable => throw EC2ClientAPIException("Error while retrieving current instance ip", e)
    }
  }

  private def getInstanceId(): Try[String] = Try {
    // Note: http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html#instancedata-data-retrieval
    val conn = new URL("http://169.254.169.254/latest/meta-data/instance-id").openConnection
    val in = new BufferedReader(new InputStreamReader(conn.getInputStream))
    try in.readLine() finally in.close()
  }

  private def getInstance(id: String): Try[Instance] = Try {
    val result = ec2.describeInstances(new DescribeInstancesRequest {
      setInstanceIds(Seq(id))
    })
    result.getReservations.head.getInstances.head
  }

}

object EC2ClientAPI {

  case class EC2ClientAPIException(msg: String, cause: Throwable) extends Exception(msg, cause)

  def apply(): EC2ClientAPI = {
    val credentials = new InstanceProfileCredentialsProvider
    val region = Region.getRegion(Regions.US_EAST_1) // Note: the region must match your EC2 instances region
    val scalingClient = new AmazonAutoScalingClient(credentials) { setRegion(region) }
    val ec2Client = new AmazonEC2Client(credentials) { setRegion(region) }
    new EC2ClientAPI(scalingClient, ec2Client)
  }
}
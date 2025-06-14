package cool.cfapps.performancedemo.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/diagnostic")
class DiagnosticController(
    @Autowired private val applicationContext: ApplicationContext
) {

    @GetMapping("/beans")
    fun listBeans(): Map<String, Any> {
        val beanNames = applicationContext.beanDefinitionNames
        val relevantBeans = beanNames.filter {
            it.contains("java", ignoreCase = true) ||
                    it.contains("kotlin", ignoreCase = true) ||
                    it.contains("performancedemo", ignoreCase = true)
        }

        val beanDetails = mutableMapOf<String, String>()

        relevantBeans.forEach { beanName ->
            try {
                val bean = applicationContext.getBean(beanName)
                beanDetails[beanName] = bean.javaClass.name
            } catch (e: Exception) {
                beanDetails[beanName] = "Error: ${e.message}"
            }
        }

        return mapOf(
            "totalBeans" to beanNames.size,
            "relevantBeans" to beanDetails,
            "allControllers" to findControllerBeans(),
            "allServices" to findServiceBeans()
        )
    }

    @GetMapping("/controllers")
    fun listControllers(): Map<String, Any> {
        return mapOf(
            "controllers" to findControllerBeans()
        )
    }

    @GetMapping("/services")
    fun listServices(): Map<String, Any> {
        return mapOf(
            "services" to findServiceBeans()
        )
    }

    private fun findControllerBeans(): Map<String, String> {
        val controllers = mutableMapOf<String, String>()
        applicationContext.getBeansWithAnnotation(org.springframework.web.bind.annotation.RestController::class.java)
            .forEach { (name, bean) ->
                controllers[name] = bean.javaClass.name
            }
        return controllers
    }

    private fun findServiceBeans(): Map<String, String> {
        val services = mutableMapOf<String, String>()
        applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Service::class.java)
            .forEach { (name, bean) ->
                services[name] = bean.javaClass.name
            }
        return services
    }

    @GetMapping("/java-status")
    fun checkJavaBeansStatus(): Map<String, Any> {
        val status = mutableMapOf<String, Any>()

        // Check for Java Controller
        try {
            val javaController = applicationContext.getBean("javaController")
            status["javaController"] = mapOf(
                "found" to true,
                "class" to javaController.javaClass.name
            )
        } catch (e: Exception) {
            status["javaController"] = mapOf(
                "found" to false,
                "error" to e.message
            )
        }

        // Check for Java Service
        try {
            val javaService = applicationContext.getBean("javaBusinessService")
            status["javaBusinessService"] = mapOf(
                "found" to true,
                "class" to javaService.javaClass.name
            )
        } catch (e: Exception) {
            status["javaBusinessService"] = mapOf(
                "found" to false,
                "error" to e.message
            )
        }

        return status
    }
}
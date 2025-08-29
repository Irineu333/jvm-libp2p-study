import io.libp2p.core.dsl.host
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.protocol.Ping

fun main() {

    print("Enter port: ")
    val port = readln().toInt()

    val node = host {
        identity { random() }
        protocols { +Ping() }
        network {
            listen("/ip4/127.0.0.1/tcp/$port")
        }
    }

    node.start().get()

    println()
    println("Node started")
    println("id: ${node.peerId}")
    println("address: ${node.listenAddresses().single()}")
    println()

    do {

        println("1. Connect to peer")
        println("2. List peers")
        println("0. Exit")
        print("> ")

        val op = readln().toInt()

        println()

        when (op) {
            1 -> {
                print("Peer address: ")
                val address = Multiaddr(readln())
                val connection = node.network.connect(address).get()
                println("Connected to ${connection.secureSession().remoteId}")
            }

            2 -> {
                val connections = node.network.connections

                if (connections.isEmpty()) {
                    println("None")
                } else {
                    println("Peers:")
                    connections.forEach {
                        println(it.secureSession().remoteId)
                    }
                }
            }
        }

        println()

    } while (op != 0)

    node.stop().get()
    println("Node stopped")
}
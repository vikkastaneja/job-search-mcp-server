$request = [System.Net.WebRequest]::Create("http://localhost:8080/sse")
$request.Accept = "text/event-stream"
$response = $request.GetResponse()
$stream = $response.GetResponseStream()
$reader = New-Object System.IO.StreamReader($stream)
while ($true) {
    $line = $reader.ReadLine()
    Write-Host $line
}   
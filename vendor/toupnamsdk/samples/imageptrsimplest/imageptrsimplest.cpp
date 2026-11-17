#ifdef _WIN32
#include <winsock2.h>
#else
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#endif
#include <stdio.h>
#include <stdlib.h>
#include "toupnam.h"

HToupnam g_hcam = NULL;
unsigned g_total = 0;

static void __stdcall EventCallback(unsigned nEvent, unsigned nPara, void* pCallbackCtx, ToupnamEventExtra* pExtra)
{
	if (nEvent == TOUPNAM_EVENT_IMAGE)
	{
		ToupnamImagePtr ptr = { 0 };
		if (SUCCEEDED(Toupnam_PullImagePtr(g_hcam, &ptr)))
		{
			printf("pull image ok, total = %u, %u x %u\n", ++g_total, ptr.width, ptr.height);
			Toupnam_PushImagePtr(g_hcam, ptr.id);
		}
		else
		{
			printf("pull image failed\n");
		}
	}
}

int main(int, char**)
{
	Toupnam_Init(EventCallback, NULL);
	printf("wait 5 seconds to discover the online cameras\n");
#ifdef _WIN32
	Sleep(5000);
#else
	sleep(5);
#endif
	ToupnamDevice arr[TOUPNAM_MAX];
    int n = Toupnam_Enum(arr, TOUPNAM_MAX);
    if (n <= 0)
    {
        printf("no camera found\n");
        return -1;
    }
    for (int i = 0; i < n; ++i)
        printf("%s: %s\n", arr[i].model, arr[i].addr);
    g_hcam = Toupnam_Open(arr[0].id);
    if (NULL == g_hcam)
    {
        printf("open camera failed\n");
        return -1;
    }
    
    int ret = Toupnam_StartPullModeWithCallback(g_hcam, NULL);
    if (ret < 0)
        printf("failed to start camera, ret = %d\n", ret);
    else
    {
        printf("press ENTER to exit\n");
        getc(stdin);
    }
    
    /* cleanup */
    Toupnam_Close(g_hcam);
	Toupnam_Fini();
    return 0;
}
